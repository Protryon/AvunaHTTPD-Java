package org.avuna.httpd.http.plugins.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.Method;
import org.avuna.httpd.http.networking.Packet;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Patch;
import org.avuna.httpd.util.Logger;
import sun.misc.BASE64Encoder;

public class PatchInline extends Patch {
	
	public PatchInline(String name) {
		super(name);
	}
	
	private String[] uas;
	private int sizeLimit = 32768;
	
	@Override
	public void formatConfig(HashMap<String, Object> json) {
		super.formatConfig(json);
		if (!json.containsKey("user-agents")) json.put("user-agents", "gecko,chrom,webkit,opera,konqueror,trident");
		if (!json.containsKey("sizeLimit")) json.put("sizeLimit", "32768");
		uas = ((String)json.get("user-agents")).trim().split(",");
		sizeLimit = Integer.parseInt((String)json.get("sizeLimit"));
	}
	
	@Override
	public boolean shouldProcessPacket(Packet packet) {
		return false;
	}
	
	@Override
	public void processPacket(Packet packet) {
		
	}
	
	@Override
	public boolean shouldProcessResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		String ua = request.headers.hasHeader("User-Agent") ? request.headers.getHeader("User-Agent").toLowerCase() : "";
		boolean g = false;
		for (String pua : uas) {
			if (ua.contains(pua)) {
				g = true;
				break;
			}
		}
		if (!g) return false;
		String ct = response.headers.hasHeader("Content-Type") ? response.headers.getHeader("Content-Type") : "";
		if (ct.length() == 0) return false;
		return response.statusCode == 200 && data != null && data.length > 0 && (ct.startsWith("text/html") || ct.startsWith("text/css"));
	}
	
	// html
	private final Pattern inlineLink = Pattern.compile("<link.*>", Pattern.CASE_INSENSITIVE);
	private final Pattern inlineImage = Pattern.compile("<img.*>", Pattern.CASE_INSENSITIVE);
	private final Pattern inlineInputImage = Pattern.compile("<input.*type=\"image\".*>", Pattern.CASE_INSENSITIVE);
	private final Pattern inlineScript = Pattern.compile("<script.*src=\".*\".*>", Pattern.CASE_INSENSITIVE);
	// css
	private final Pattern inlineCSS = Pattern.compile("url\\([^\\)]*", Pattern.CASE_INSENSITIVE);
	
	private final HashMap<String, String> cacheBase64 = new HashMap<String, String>();
	private final HashMap<String, Boolean> tooBig = new HashMap<String, Boolean>();
	
	public void clearCache() {
		cacheBase64.clear();
		subreqs.clear();
		cdata.clear();
	}
	
	private final BASE64Encoder encoder = new BASE64Encoder();
	private final Comparator<SubReq> subReqComparator = new Comparator<SubReq>() {
		public int compare(SubReq x, SubReq y) {
			return x.start - y.start;
		}
	};
	
	private static String processHREF(String parent, String href) {
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
			Logger.log("[WARNING] Attempt to escape htdocs from Inline: " + parent + " child: " + href);
			return null;
		}
		String[] f = new String[ps.length - pt + hs.length - pt];
		System.arraycopy(ps, 0, f, 0, ps.length - pt);
		System.arraycopy(hs, pt, f, ps.length - pt, hs.length - pt);
		h = "";
		for (String s : f) {
			if (s.length() == 0) continue;
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
		public final int start, end;
		public final String orig, forig;
		
		public SubReq(String req, int start, int end, String orig, String forig) {
			this.req = req;
			this.start = start;
			this.end = end;
			this.orig = orig;
			this.forig = forig;
		}
	}
	
	private final HashMap<Long, SubReq[]> subreqs = new HashMap<Long, SubReq[]>();
	private final HashMap<Long, byte[]> cdata = new HashMap<Long, byte[]>();
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		CRC32 process = new CRC32();
		process.update(data);
		long l = process.getValue();
		if (cdata.containsKey(l)) {
			return cdata.get(l);
		}
		String html = new String(data); // TODO: encoding support
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
					href = processHREF(request.target, href);
					if (href == null) continue;
					genreqs.add(new SubReq(href, mtch.start(), mtch.end(), oh, o));
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
					href = processHREF(request.target, href);
					if (href == null) continue;
					genreqs.add(new SubReq(href, mtch.start(), mtch.end(), oh, o));
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
					href = processHREF(request.target, href);
					if (href == null) continue;
					genreqs.add(new SubReq(href, mtch.start(), mtch.end(), oh, o));
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
					href = processHREF(request.target, href);
					if (href == null) continue;
					genreqs.add(new SubReq(href, mtch.start(), mtch.end(), oh, o));
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
					href = processHREF(request.target, href);
					if (href == null) continue;
					genreqs.add(new SubReq(href, mtch.start(), mtch.end(), oh, o));
				}
			}
			for (int i = 0; i < genreqs.size(); i++) {
				if (tooBig.containsKey(genreqs.get(i).req) && tooBig.get(genreqs.get(i).req).equals(true)) {
					genreqs.remove(i);
					i--;
				}
			}
			Collections.sort(genreqs, subReqComparator);
			SubReq[] sa = genreqs.toArray(new SubReq[0]);
			subreqs = sa;
			greqs = true;
		}
		RequestPacket[] reqs = new RequestPacket[subreqs.length];
		for (int i = 0; i < subreqs.length; i++) {
			RequestPacket subreq = request.clone();
			subreq.parent = request;
			subreq.target = subreqs[i].req;
			subreq.method = Method.GET;
			subreq.body.data = null;
			subreq.headers.removeHeaders("If-None-Matches"); // just in case of collision + why bother ETag?
			subreq.headers.removeHeaders("Accept-Encoding"); // gzip = problem
			reqs[i] = subreq;
		}
		ResponsePacket[] resps = request.host.getHost().processSubRequests(reqs);
		if (greqs) {
			SubReq[] srsn = new SubReq[subreqs.length];
			ResponsePacket[] respsn = new ResponsePacket[resps.length];
			int nl = 0;
			for (int i = 0; i < reqs.length; i++) {
				if (!(resps[i] == null || resps[i].subwrite == null || resps[i].subwrite.length > sizeLimit)) {
					srsn[nl] = subreqs[i];
					respsn[nl++] = resps[i];
					tooBig.put(subreqs[i].req, false);
				}else {
					tooBig.put(subreqs[i].req, true);
				}
			}
			subreqs = new SubReq[nl];
			resps = new ResponsePacket[nl];
			System.arraycopy(srsn, 0, subreqs, 0, nl);
			System.arraycopy(respsn, 0, resps, 0, nl);
			this.subreqs.put(l, subreqs);
		}
		int offset = 0;
		for (int i = 0; i < resps.length; i++) {
			if (resps[i] == null || resps[i].subwrite == null) continue;
			SubReq sr = subreqs[i];
			String base64 = "";
			String cachePath = resps[i].request.host.getHostPath() + resps[i].request.target;
			if (!cacheBase64.containsKey(cachePath)) {
				base64 = new BASE64Encoder().encode(resps[i].subwrite).replace(AvunaHTTPD.crlf, "");
				cacheBase64.put(cachePath, base64);
			}else {
				base64 = cacheBase64.get(cachePath);
			}
			String rep = "data:" + resps[i].headers.getHeader("Content-Type") + ";base64," + base64;
			rep = sr.forig.replace(sr.orig, rep);
			html = html.substring(0, sr.start + offset) + rep + html.substring(sr.end + offset);
			offset += rep.length() - sr.forig.length();
		}
		byte[] hb = html.getBytes();
		cdata.put(l, hb);
		return hb;
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
	
}
