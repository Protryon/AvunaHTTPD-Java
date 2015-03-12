package com.javaprophet.javawebserver.plugins.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.misc.BASE64Encoder;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.ThreadWorker;
import com.javaprophet.javawebserver.networking.packets.Packet;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;
import com.javaprophet.javawebserver.plugins.Patch;

public class PatchInline extends Patch {
	
	public PatchInline(String name) {
		super(name);
	}
	
	@Override
	public void formatConfig(HashMap<String, Object> json) {
		
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
		if (!ua.contains("gecko") && !ua.contains("chrom") && !ua.contains("webkit") && !ua.contains("opera") && !ua.contains("konqueror") && !ua.contains("trident")) return false;
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
	private final Pattern inlineCSS = Pattern.compile("url\\(.*\\)", Pattern.CASE_INSENSITIVE);
	private final BASE64Encoder encoder = new BASE64Encoder();
	private final Comparator<SubReq> subReqComparator = new Comparator<SubReq>() {
		public int compare(SubReq x, SubReq y) {
			return x.start - y.start;
		}
	};
	
	private static String processHREF(String href) {
		String h = href;
		if (h.startsWith("http://") || h.startsWith("https://") || h.startsWith("//")) {
			return null; // don't both with offsite stuff, will only increase response time TODO: onsite hard-linking?
		}
		if (!h.startsWith("/")) h = "/" + h;
		String th = h;
		if (th.contains("#")) th = th.substring(0, th.indexOf("#"));
		if (th.contains("?")) th = th.substring(0, th.indexOf("?"));
		if (th.endsWith(".css") || th.endsWith(".js") || th.endsWith(".png") || th.endsWith(".png") || th.endsWith(".jpg") || th.endsWith(".gif")) {
			return h;
		}else {
			return null; // dont want to mess up other stuff
		}
	}
	
	private static class SubReq {
		public final RequestPacket req;
		public final int start, end;
		public final String orig, forig;
		
		public SubReq(RequestPacket req, int start, int end, String orig, String forig) {
			this.req = req;
			this.start = start;
			this.end = end;
			this.orig = orig;
			this.forig = forig;
		}
	}
	
	@Override
	public byte[] processResponse(ResponsePacket response, RequestPacket request, byte[] data) {
		String html = new String(data); // TODO: encoding support
		ArrayList<SubReq> subreqs = new ArrayList<SubReq>();
		String ct = response.headers.getHeader("Content-Type");
		if (ct.startsWith("text/html")) {
			Matcher mtch = inlineLink.matcher(html);
			while (mtch.find()) {
				String o = mtch.group();
				if (!o.contains("href=")) continue;
				String href = o.substring(o.indexOf("href=") + 5);
				if (href.startsWith("\"")) {
					href = href.substring(1, href.indexOf("\"", 1));
				}else {
					href = href.substring(0, href.indexOf(" "));
				}
				String oh = href;
				href = processHREF(href);
				if (href == null) continue;
				RequestPacket subreq = request.clone();
				subreq.parent = request;
				subreq.target = href;
				subreq.headers.removeHeaders("If-None-Matches"); // just in case of collision + why bother ETag?
				subreq.headers.removeHeaders("Accept-Encoding"); // gzip = problem
				subreqs.add(new SubReq(subreq, mtch.start(), mtch.end(), oh, o));
			}
			mtch = inlineImage.matcher(html);
			while (mtch.find()) {
				String o = mtch.group();
				if (!o.contains("src=")) continue;
				String href = o.substring(o.indexOf("src=") + 4);
				if (href.startsWith("\"")) {
					href = href.substring(1, href.indexOf("\"", 1));
				}else {
					href = href.substring(0, href.indexOf(" "));
				}
				String oh = href;
				href = processHREF(href);
				if (href == null) continue;
				RequestPacket subreq = request.clone();
				subreq.parent = request;
				subreq.target = href;
				subreq.headers.removeHeaders("If-None-Matches"); // just in case of collision + why bother ETag?
				subreq.headers.removeHeaders("Accept-Encoding"); // gzip = problem
				subreqs.add(new SubReq(subreq, mtch.start(), mtch.end(), oh, o));
			}
			mtch = inlineInputImage.matcher(html);
			while (mtch.find()) {
				String o = mtch.group();
				if (!o.contains("src=")) continue;
				String href = o.substring(o.indexOf("src=") + 4);
				if (href.startsWith("\"")) {
					href = href.substring(1, href.indexOf("\"", 1));
				}else {
					href = href.substring(0, href.indexOf(" "));
				}
				String oh = href;
				href = processHREF(href);
				if (href == null) continue;
				RequestPacket subreq = request.clone();
				subreq.parent = request;
				subreq.target = href;
				subreq.headers.removeHeaders("If-None-Matches"); // just in case of collision + why bother ETag?
				subreq.headers.removeHeaders("Accept-Encoding"); // gzip = problem
				subreqs.add(new SubReq(subreq, mtch.start(), mtch.end(), oh, o));
			}
			mtch = inlineScript.matcher(html);
			while (mtch.find()) {
				String o = mtch.group();
				if (!o.contains("src=")) continue;
				String href = o.substring(o.indexOf("src=") + 4);
				if (href.startsWith("\"")) {
					href = href.substring(1, href.indexOf("\"", 1));
				}else {
					href = href.substring(0, href.indexOf(" "));
				}
				String oh = href;
				href = processHREF(href);
				if (href == null) continue;
				RequestPacket subreq = request.clone();
				subreq.parent = request;
				subreq.target = href;
				subreq.headers.removeHeaders("If-None-Matches"); // just in case of collision + why bother ETag?
				subreq.headers.removeHeaders("Accept-Encoding"); // gzip = problem
				subreqs.add(new SubReq(subreq, mtch.start(), mtch.end(), oh, o));
			}
		}else if (ct.startsWith("text/css")) {
			Matcher mtch = inlineCSS.matcher(html);
			while (mtch.find()) {
				String o = mtch.group();
				if (!o.contains("url(")) continue;
				String href = o.substring(o.indexOf("url(") + 4); // 0 + 4 :)
				if (href.startsWith("\"")) {
					href = href.substring(1, href.indexOf("\"", 1));
				}else {
					href = href.substring(0, href.indexOf(")"));
				}
				String oh = href;
				href = processHREF(href);
				if (href == null) continue;
				RequestPacket subreq = request.clone();
				subreq.parent = request;
				subreq.target = href;
				subreq.headers.removeHeaders("If-None-Matches"); // just in case of collision + why bother ETag?
				subreq.headers.removeHeaders("Accept-Encoding"); // gzip = problem
				subreqs.add(new SubReq(subreq, mtch.start(), mtch.end(), oh, o));
			}
		}
		Collections.sort(subreqs, subReqComparator);
		SubReq[] sa = subreqs.toArray(new SubReq[0]);
		RequestPacket[] reqs = new RequestPacket[sa.length];
		for (int i = 0; i < sa.length; i++) {
			reqs[i] = sa[i].req;
		}
		ResponsePacket[] resps = ThreadWorker.processSubRequests(reqs);
		for (ResponsePacket subreq : resps) {
			if (subreq == null || subreq.subwrite == null) continue;
		}
		int offset = 0;
		for (int i = 0; i < resps.length; i++) {
			if (resps[i] == null || resps[i].subwrite == null) continue;
			SubReq sr = subreqs.get(i);
			String rep = "data:" + resps[i].headers.getHeader("Content-Type") + ";base64," + new BASE64Encoder().encode(resps[i].subwrite).replace(JavaWebServer.crlf, "");
			rep = sr.forig.replace(sr.orig, rep);
			html = html.substring(0, sr.start + offset) + rep + html.substring(sr.end + offset);
			offset += rep.length() - sr.forig.length();
		}
		return html.getBytes();
	}
	
	@Override
	public void processMethod(RequestPacket request, ResponsePacket response) {
		
	}
	
}
