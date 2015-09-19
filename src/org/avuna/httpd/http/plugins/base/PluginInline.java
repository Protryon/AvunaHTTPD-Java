/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.base;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.hosts.VHost;
import org.avuna.httpd.http.Method;
import org.avuna.httpd.http.event.EventClearCache;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.StringUtil;
import sun.misc.BASE64Encoder;

public class PluginInline extends Plugin {
	
	public PluginInline(String name, PluginRegistry registry, File config) {
		super(name, registry, config);
	}
	
	private String[] uas;
	private int sizeLimit = 32768;
	
	@Override
	public void formatConfig(ConfigNode json) {
		super.formatConfig(json);
		if (!json.containsNode("user-agents")) json.insertNode("user-agents", "gecko,chrom,webkit,opera,konqueror,trident");
		if (!json.containsNode("sizeLimit")) json.insertNode("sizeLimit", "32768");
		uas = (json.getNode("user-agents").getValue()).trim().split(",");
		sizeLimit = Integer.parseInt(json.getNode("sizeLimit").getValue());
	}
	
	public boolean shouldProcessResponse(ResponsePacket response, RequestPacket request) {
		String ua = request.headers.getHeader("User-Agent");
		if (ua == null) return false;
		ua = StringUtil.toLowerCase(ua);
		if (!StringUtil.containsAny(ua, uas)) return false;
		String ct = response.headers.getHeader("Content-Type");
		if (ct == null) return false;
		return response.statusCode == 200 && response.hasContent() && (ct.startsWith("text/html") || ct.startsWith("text/css"));
	}
	
	// html
	private final Pattern inlineLink = Pattern.compile("<link.*>", Pattern.CASE_INSENSITIVE);
	private final Pattern inlineImage = Pattern.compile("<img.*>", Pattern.CASE_INSENSITIVE);
	private final Pattern inlineInputImage = Pattern.compile("<input.*type=\"image\".*>", Pattern.CASE_INSENSITIVE);
	private final Pattern inlineScript = Pattern.compile("<script.*src=\".*\".*>", Pattern.CASE_INSENSITIVE);
	// css
	private final Pattern inlineCSS = Pattern.compile("url\\([^\\)]*", Pattern.CASE_INSENSITIVE);
	
	private final HashMap<String, String> cacheBase64 = new HashMap<String, String>();
	
	private final Comparator<SubReq> subReqComparator = new Comparator<SubReq>() {
		public int compare(SubReq x, SubReq y) {
			return x.start - y.start;
		}
	};
	
	private static String processHREF(VHost vhost, String parent, String href) {
		String h = href.trim();
		if (h.startsWith("http://") || h.startsWith("https://") || h.startsWith("//")) {
			return null; // don't both with offsite stuff, will only increase response time TODO: onsite hard-linking?
		}
		String[] hs = h.split("/");
		String[] ps = parent.substring(0, parent.lastIndexOf("/")).split("/");
		int pt = 0;
		for (int i = 0; i < hs.length; i++) {
			if (hs[i].length() == 0) continue;
			if (hs[i].equals("..")) {
				pt++;
			}else {
				break;
			}
		}
		if (pt > ps.length) {
			vhost.logger.log("[WARNING] Attempt to escape htdocs from Inline: " + parent + " child: " + href);
			return null;
		}
		String[] f = new String[ps.length - pt + hs.length - pt];
		System.arraycopy(ps, 0, f, 0, ps.length - pt);
		System.arraycopy(hs, pt, f, ps.length - pt, hs.length - pt);
		h = "";
		for (String s : f) {
			if (s.length() == 0 || s.equals(".")) continue;
			h += "/" + s;
		}
		if (!h.startsWith("/")) h = "/" + h;
		String th = h;
		if (th.contains("#")) th = th.substring(0, th.indexOf("#"));
		if (th.contains("?")) th = th.substring(0, th.indexOf("?"));
		if (th.endsWith(".css") || th.endsWith(".js") || th.endsWith(".png") || th.endsWith(".jpg") || th.endsWith(".gif") || th.endsWith(".eot") || th.endsWith(".svg") || th.endsWith(".ttf") || th.endsWith(".woff") || th.endsWith(".woff2")) {
			return h;
		}else {
			return null; // dont want to mess up other stuff
		}
	}
	
	private static class SubReq {
		public final String req;
		public final SubReq copy;
		public int start, end;
		public final String orig, forig;
		public int size;
		public boolean failed = false;
		
		public SubReq(String req, int start, int end, String orig, String forig, int size) {
			this.req = req;
			this.copy = null;
			this.start = start;
			this.end = end;
			this.orig = orig;
			this.forig = forig;
			this.size = size;
		}
		
		public SubReq(SubReq copy, int start, int end, String orig, String forig, int size) {
			this.req = null;
			this.copy = copy;
			this.start = start;
			this.end = end;
			this.orig = orig;
			this.forig = forig;
			this.size = size;
		}
	}
	
	private final HashMap<Long, SubReq[]> subreqs = new HashMap<Long, SubReq[]>();
	private final HashMap<Long, byte[]> cdata = new HashMap<Long, byte[]>();
	private final ArrayList<Long> skip = new ArrayList<Long>();
	
	public void processResponse(ResponsePacket response, RequestPacket request) {
		CRC32 process = new CRC32();
		process.update(response.body.data);
		long l = process.getValue();
		if (skip.contains(l)) return;
		if (cdata.containsKey(l)) {
			String s = new String(cdata.get(l));
			s = s.replaceAll("(?<=[^\\\\])\\\\h", request.headers.getHeader("Host"));
			response.body.data = StringUtil.unescape(s, new char[0]).getBytes();
			return;
		}
		String html = new String(response.body.data); // TODO: encoding support
		html = StringUtil.escape(html, new char[0]);
		boolean greqs = false;
		SubReq[] subreqs = null;
		if (this.subreqs.containsKey(l)) {
			subreqs = this.subreqs.get(l);
		}else {
			ArrayList<SubReq> genreqs = new ArrayList<SubReq>();
			String ct = response.headers.getHeader("Content-Type");
			if (ct.startsWith("text/html")) {
				Matcher mtch = inlineLink.matcher(html);
				while (mtch.find()) {
					String o = mtch.group();
					if (!o.contains("href=")) continue;
					String href = o.substring(o.indexOf("href=") + 5);
					if (href.startsWith("\"")) {
						href = href.substring(1, href.indexOf("\"", 1));
					}else if (href.startsWith("'")) {
						href = href.substring(1, href.indexOf("'", 1));
					}else {
						href = href.substring(0, href.indexOf(" "));
					}
					String oh = href;
					href = processHREF(request.host, request.target, href);
					if (href == null) continue;
					SubReq cp = null;
					for (SubReq sr : genreqs) {
						if (sr.req != null && sr.req.equals(href)) {
							cp = sr;
							break;
						}
					}
					genreqs.add(cp == null ? new SubReq(href, mtch.start(), mtch.end(), oh, o, -1) : new SubReq(cp, mtch.start(), mtch.end(), oh, o, -1));
				}
				mtch = inlineImage.matcher(html);
				while (mtch.find()) {
					String o = mtch.group();
					if (!o.contains("src=")) continue;
					String href = o.substring(o.indexOf("src=") + 4);
					if (href.startsWith("\"")) {
						href = href.substring(1, href.indexOf("\"", 1));
					}else if (href.startsWith("'")) {
						href = href.substring(1, href.indexOf("'", 1));
					}else {
						href = href.substring(0, href.indexOf(" "));
					}
					String oh = href;
					href = processHREF(request.host, request.target, href);
					if (href == null) continue;
					SubReq cp = null;
					for (SubReq sr : genreqs) {
						if (sr.req != null && sr.req.equals(href)) {
							cp = sr;
							break;
						}
					}
					genreqs.add(cp == null ? new SubReq(href, mtch.start(), mtch.end(), oh, o, -1) : new SubReq(cp, mtch.start(), mtch.end(), oh, o, -1));
				}
				mtch = inlineInputImage.matcher(html);
				while (mtch.find()) {
					String o = mtch.group();
					if (!o.contains("src=")) continue;
					String href = o.substring(o.indexOf("src=") + 4);
					if (href.startsWith("\"")) {
						href = href.substring(1, href.indexOf("\"", 1));
					}else if (href.startsWith("'")) {
						href = href.substring(1, href.indexOf("'", 1));
					}else {
						href = href.substring(0, href.indexOf(" "));
					}
					String oh = href;
					href = processHREF(request.host, request.target, href);
					if (href == null) continue;
					SubReq cp = null;
					for (SubReq sr : genreqs) {
						if (sr.req != null && sr.req.equals(href)) {
							cp = sr;
							break;
						}
					}
					genreqs.add(cp == null ? new SubReq(href, mtch.start(), mtch.end(), oh, o, -1) : new SubReq(cp, mtch.start(), mtch.end(), oh, o, -1));
				}
				mtch = inlineScript.matcher(html);
				while (mtch.find()) {
					String o = mtch.group();
					if (!o.contains("src=")) continue;
					String href = o.substring(o.indexOf("src=") + 4);
					if (href.startsWith("\"")) {
						href = href.substring(1, href.indexOf("\"", 1));
					}else if (href.startsWith("'")) {
						href = href.substring(1, href.indexOf("'", 1));
					}else {
						href = href.substring(0, href.indexOf(" "));
					}
					String oh = href;
					href = processHREF(request.host, request.target, href);
					if (href == null) continue;
					SubReq cp = null;
					for (SubReq sr : genreqs) {
						if (sr.req != null && sr.req.equals(href)) {
							cp = sr;
							break;
						}
					}
					genreqs.add(cp == null ? new SubReq(href, mtch.start(), mtch.end(), oh, o, -1) : new SubReq(cp, mtch.start(), mtch.end(), oh, o, -1));
				}
			}else if (ct.startsWith("text/css")) {
				Matcher mtch = inlineCSS.matcher(html);
				while (mtch.find()) {
					String o = mtch.group();
					if (!o.contains("url(")) continue;
					String href = o.substring(o.indexOf("url(") + 4); // 0 + 4 :)
					if (href.startsWith("\"")) {
						href = href.substring(1, href.indexOf("\"", 1));
					}else if (href.startsWith("'")) {
						href = href.substring(1, href.indexOf("'", 1));
					}else {
						href = href.contains(")") ? href.substring(0, href.indexOf(")")) : href;
					}
					String oh = href;
					href = processHREF(request.host, request.target, href);
					if (href == null) continue;
					SubReq cp = null;
					for (SubReq sr : genreqs) {
						if (sr.req != null && sr.req.equals(href)) {
							cp = sr;
							break;
						}
					}
					genreqs.add(cp == null ? new SubReq(href, mtch.start(), mtch.end(), oh, o, -1) : new SubReq(cp, mtch.start(), mtch.end(), oh, o, -1));
				}
			}
			Collections.sort(genreqs, subReqComparator);
			SubReq[] sa = genreqs.toArray(new SubReq[0]);
			subreqs = sa;
			greqs = true;
		}
		RequestPacket[] reqs = new RequestPacket[subreqs.length];
		int ri = 0;
		for (int i = 0; i < subreqs.length; i++) {
			if (subreqs[i].size > sizeLimit) {
				continue;
			}
			if (subreqs[i].copy != null) {
				reqs[ri++] = null;
				continue;
			}
			RequestPacket subreq = request.clone();
			subreq.parent = request;
			subreq.target = subreqs[i].req;
			subreq.method = Method.GET;
			subreq.body.data = null;
			subreq.headers.removeHeaders("If-None-Matches"); // just in case of collision + why bother ETag?
			subreq.headers.removeHeaders("Accept-Encoding"); // gzip = problem
			reqs[ri++] = subreq;
		}
		if (ri < reqs.length) {
			RequestPacket[] rp = new RequestPacket[ri];
			System.arraycopy(reqs, 0, rp, 0, ri);
			reqs = rp;
		}
		ResponsePacket[] resps = reqs.length == 0 ? new ResponsePacket[0] : request.host.getHost().processSubRequests(reqs);
		if (greqs) {
			SubReq[] srsn = new SubReq[subreqs.length];
			ResponsePacket[] respsn = new ResponsePacket[resps.length];
			int nl = 0;
			for (int i = 0; i < reqs.length; i++) {
				subreqs[i].size = resps[i] == null ? subreqs[i].copy.size : resps[i].subwrite.length;
				if (subreqs[i].size > sizeLimit) {
					subreqs[i].failed = true;
				}
				srsn[nl] = subreqs[i];
				respsn[nl++] = resps[i];
			}
			subreqs = new SubReq[nl];
			resps = new ResponsePacket[nl];
			System.arraycopy(srsn, 0, subreqs, 0, nl);
			System.arraycopy(respsn, 0, resps, 0, nl);
			this.subreqs.put(l, subreqs);// put back above?
		}
		int offset = 0;
		if (resps.length == 0) {
			skip.add(l);
			return;
		}
		for (int i = 0; i < resps.length; i++) {
			SubReq sr = subreqs[i];
			boolean fre = sr.failed;
			String rep = null;
			if (fre) {
				rep = "http" + (request.work.ssl ? "s" : "") + "://\\h" + (sr.copy == null ? sr.req : sr.copy.req);
			}else {
				if (resps[i] == null) {
					for (int i2 = 0; i2 < i; i2++) {
						if (subreqs[i2] == subreqs[i].copy) {
							resps[i] = resps[i2];
						}
					}
				}
				if (resps[i].subwrite == null) continue;
				String base64 = "";
				String cachePath = resps[i].request.host.getHostPath() + resps[i].request.target + request.headers.getHeader("Host");
				if (!cacheBase64.containsKey(cachePath)) {
					byte[] srb = resps[i].subwrite;
					String ct = resps[i].headers.getHeader("Content-Type");
					if (ct != null && resps[i].statusCode == 200 && resps[i].body != null && (ct.startsWith("text/html") || ct.startsWith("text/css"))) {
						String srs = new String(srb);
						srs = srs.replaceAll("(?<=[^\\\\])\\\\h", request.headers.getHeader("Host"));
						srs = StringUtil.unescape(srs, new char[0]);
						srb = srs.getBytes();
					}
					base64 = new BASE64Encoder().encode(srb).replace(AvunaHTTPD.crlf, "");
					cacheBase64.put(cachePath, base64);
				}else {
					base64 = cacheBase64.get(cachePath);
				}
				rep = "data:" + resps[i].headers.getHeader("Content-Type") + ";base64," + base64;
			}
			rep = sr.forig.replace(sr.orig, rep);
			html = html.substring(0, sr.start + offset) + rep + html.substring(sr.end + offset);
			offset += rep.length() - sr.forig.length();
		}
		byte[] hb = html.getBytes();
		cdata.put(l, hb);
		if (request.parent == null) {
			html = html.replaceAll("(?<=[^\\\\])\\\\h", request.headers.getHeader("Host"));
			html = StringUtil.unescape(html, new char[0]);
			hb = html.getBytes();
		}
		response.body.data = hb;
	}
	
	@Override
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventGenerateResponse) {
			EventGenerateResponse egr = (EventGenerateResponse) event;
			ResponsePacket response = egr.getResponse();
			RequestPacket request = egr.getRequest();
			if (shouldProcessResponse(response, request)) processResponse(response, request);
		}else if (event instanceof EventClearCache) {
			cacheBase64.clear();
			subreqs.clear();
			cdata.clear();
		}
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.GENERATERESPONSE, this, -600);
		bus.registerEvent(HTTPEventID.CLEARCACHE, this, 0);
	}
	
}
