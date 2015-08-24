/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.base;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.hosts.VHost;
import org.avuna.httpd.http.Method;
import org.avuna.httpd.http.event.EventGenerateResponse;
import org.avuna.httpd.http.event.HTTPEventID;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.plugins.Plugin;
import org.avuna.httpd.http.plugins.PluginRegistry;

public class PluginSSI extends Plugin {
	
	public PluginSSI(String name, PluginRegistry registry, File config) {
		super(name, registry, config);
	}
	
	private static String processHREF(VHost vhost, String parent, String href) {
		String h = href.trim();
		if (h.startsWith("http://") || h.startsWith("https://") || h.startsWith("//")) {
			return null; // cant SSI remote
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
			vhost.logger.log("[WARNING] Attempt to escape htdocs from SSI: " + parent + " child: " + href);
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
		return h;
	}
	
	private final Pattern ssiDirective = Pattern.compile("<!--\\s*#([a-zA-Z]*)\\s+(.*?)-->");
	
	private static final class ParsedDirective {
		public String directive;
		public String[] args;
		public int start = 0;
	}
	
	private final HashMap<Long, ParsedDirective[]> dirCache = new HashMap<Long, ParsedDirective[]>();
	
	@Override
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventGenerateResponse) {
			EventGenerateResponse egr = (EventGenerateResponse) event;
			ResponsePacket response = egr.getResponse();
			RequestPacket request = egr.getRequest();
			if (!response.headers.hasHeader("Content-Type") || response.body == null || response.body.data == null || response.body.data.length == 0) return;
			String ct = response.headers.getHeader("Content-Type");
			if (ct == null || !ct.startsWith("application/x-ssi")) return;
			response.headers.updateHeader("Content-Type", "text/html; charset=utf-8");
			String body = new String(response.body.data);
			CRC32 crc32 = new CRC32();
			crc32.update(response.body.data);
			ParsedDirective[] dirs = null;
			long crc = crc32.getValue();
			if (dirCache.containsKey(crc)) {
				dirs = dirCache.get(crc);
			}else {
				Matcher m = ssiDirective.matcher(body);
				ArrayList<ParsedDirective> ldirs = new ArrayList<ParsedDirective>();
				while (m.find()) {
					String directive = m.group(1);
					String dargs = m.group(2);
					ArrayList<String> args = new ArrayList<String>();
					int sl = 0;
					int stage = 0;
					String cur = "";
					while (sl < dargs.length()) {
						if (stage == 0) {
							int t = dargs.indexOf("=", sl);
							if (t < sl) break;
							cur = dargs.substring(sl, t + 1); // inc =
							sl += cur.length();
							cur = cur.trim();
							stage++;
						}else if (stage == 1) {
							boolean esc = false;
							sl = dargs.indexOf('"', sl) + 1; // skip ahead past next "
							int s = sl;
							while (sl < dargs.length()) {
								char c = dargs.charAt(sl);
								if (c == '\\') {
									esc = !esc;
								}else if (!esc) {
									if (c == '"') { // found unescaped terminator
										break;
									}
								}else {
									esc = false;
								}
								sl++;
							}
							cur += dargs.substring(s, sl); // name=value , no quotes
							args.add(cur);
							stage = 0;
						}
					}
					ParsedDirective pd = new ParsedDirective();
					pd.directive = directive;
					pd.args = args.toArray(new String[0]);
					pd.start = m.start();
					ldirs.add(pd);
				}
				dirs = ldirs.toArray(new ParsedDirective[0]);
				dirCache.put(crc, dirs);
			}
			StringBuilder res = new StringBuilder();
			int le = 0;
			if (dirs != null) {
				for (ParsedDirective pd : dirs) {
					res.append(body.substring(le, pd.start));
					if (pd.directive.equals("include")) {
						if (pd.args.length != 1) continue;
						String f = pd.args[0];
						if (f.startsWith("file=")) {
							f = processHREF(request.host, request.target, f.substring(5));
						}else if (f.startsWith("virtual=")) {
							f = f.substring(8);
							if (!f.startsWith("/")) f = "/" + f;
						}
						RequestPacket subreq = request.clone();
						subreq.parent = request;
						subreq.target = f;
						subreq.method = Method.GET;
						subreq.body.data = null;
						subreq.headers.removeHeaders("If-None-Matches"); // just in case of collision + why bother ETag?
						subreq.headers.removeHeaders("Accept-Encoding"); // gzip = problem
						ResponsePacket subresp = request.host.getHost().processSubRequests(subreq)[0];
						if (subresp != null && subresp.body != null && subresp.body.data != null) {
							res.append(new String(subresp.body.data));
						}
					}else if (pd.directive.equals("exec")) {
						if (pd.args.length != 1) continue;
						if (pd.args[0].startsWith("cgi=")) {
							String f = pd.args[0].substring(4);
							if (!f.startsWith("/")) f = "/" + f;
							RequestPacket subreq = request.clone();
							subreq.parent = request;
							subreq.target = f;
							subreq.method = Method.GET;
							subreq.body.data = null;
							subreq.headers.removeHeaders("If-None-Matches"); // just in case of collision + why bother ETag?
							subreq.headers.removeHeaders("Accept-Encoding"); // gzip = problem
							ResponsePacket subresp = request.host.getHost().processSubRequests(subreq)[0];
							if (subresp != null && subresp.body != null && subresp.body.data != null) {
								res.append(new String(subresp.body.data));
							}
						}else request.host.logger.logError("Attempted SSI Exec, potential security breach: " + pd.args[0]);
					}
					
				}
				res.append(body.substring(le, body.length()));
				response.body.data = res.toString().getBytes();
			}
		}
	}
	
	@Override
	public void register(EventBus bus) {
		bus.registerEvent(HTTPEventID.GENERATERESPONSE, this, -700);
		bus.registerEvent(HTTPEventID.CLEARCACHE, this, 0);
	}
	
}
