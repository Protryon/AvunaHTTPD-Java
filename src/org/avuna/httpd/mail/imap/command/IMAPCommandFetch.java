/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Scanner;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.http.plugins.avunaagent.lib.Multipart.MultiPartData;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;
import org.avuna.httpd.mail.mailbox.Email;
import org.avuna.httpd.mail.util.StringFormatter;

public class IMAPCommandFetch extends IMAPCommand {
	
	public IMAPCommandFetch(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	protected static void trim(StringBuilder sb) {
		String s = sb.toString().trim();
		sb.setLength(0);
		sb.append(s);
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		if (args.length >= 2) {
			ArrayList<Email> toFetch = focus.selectedMailbox.getByIdentifier(args[0]);
			String tp = args[1];
			if (tp.startsWith("(")) tp = tp.substring(1, tp.length() - 1);
			String[] tps = tp.split(" ");
			tps = StringFormatter.congealBySurroundings(tps, "[", "]");
			for (String tp2 : tps) {
				host.logger.log(tp2);
			}
			for (Email e : toFetch) {
				if (tps[0].equals("all")) {
					tps = new String[] { "FLAGS", "INTERNALDATE", "RFC822.SIZE", "ENVELOPE" };
				}else if (tps[0].equals("fast")) {
					tps = new String[] { "FLAGS", "INTERNALDATE", "RFC822.SIZE" };
				}else if (tps[0].equals("full")) {
					tps = new String[] { "FLAGS", "INTERNALDATE", "RFC822.SIZE", "ENVELOPE", "BODY" };
				}
				StringBuilder ret = new StringBuilder().append(e.uid).append(" FETCH (");
				boolean notrim = false;
				for (int i3 = 0; i3 < tps.length; i3++) {
					String s3 = tps[i3];
					String s = s3.toLowerCase();
					if (s.equals("bodystructure") || s.equals("body")) {
						ret.append("BODYSTRUCTURE (");
						if (e.mp != null) {
							for (int i2 = 0; i2 < e.mp.mpds.size(); i2++) {
								MultiPartData mpd = e.mp.mpds.get(i2);
								if (i2 == 0) {
									// some random thing some clients add, confuses some simple clients.
									continue;
								}
								String ct = mpd.contentType;
								if (ct == null) continue;
								boolean ecs = ct.contains(";");
								String ct1 = ecs ? ct.substring(0, ct.indexOf(";")).trim() : ct;
								ret.append("(");
								ret.append("\"").append(ct1.toUpperCase().replace("/", "\" \"")).append("\"");
								boolean f1 = false;
								if (ecs) {
									f1 = true;
									ret.append(" (");
								}
								while (ecs) {
									ct = ct.substring(ct.indexOf(";") + 1);
									ecs = ct.contains(";");
									ct1 = ecs ? ct.substring(0, ct.indexOf(";")).toUpperCase().trim() : ct.toUpperCase().trim();
									ret.append("\"").append(ct1.replace("\"", "").replace("=", "\" \"")).append("\"");
									if (ecs) ret.append(" ");
								}
								if (f1) {
									ret.append(")");
								}
								ret.append(" NIL NIL");
								String cte = mpd.contentTransferEncoding;
								if (cte == null) cte = "7BIT";
								ret.append(" \"").append(cte.toUpperCase()).append("\"");
								ret.append(" ").append(mpd.data.length);
								int lines = 0;
								if (mpd.data.length > 1) for (int i = 1; i < mpd.data.length; i++) {
									if (mpd.data[i - 1] == AvunaHTTPD.crlfb[0] && mpd.data[i] == AvunaHTTPD.crlfb[1]) {// assumes crlf, however, is always crlf
										lines++;
									}
								}
								ret.append(" ").append(lines);
								ret.append(")");
							}
							ret.append(" \"" + e.mp.mct.substring(e.mp.mct.indexOf("/") + 1).toUpperCase() + "\" (\"BOUNDARY\" \"");
							ret.append(e.mp.boundary);
							ret.append("\") NIL NIL");
						}else {
							String ct = e.headers.getHeader("Content-Type");
							if (ct == null) continue;
							boolean ecs = ct.contains(";");
							String ct1 = ecs ? ct.substring(0, ct.indexOf(";")).trim() : ct;
							ret.append("\"").append(ct1.toUpperCase().replace("/", "\" \"")).append("\"");
							boolean f1 = false;
							if (ecs) {
								f1 = true;
								ret.append(" (");
							}
							while (ecs) {
								ct = ct.substring(ct.indexOf(";") + 1);
								ecs = ct.contains(";");
								ct1 = ecs ? ct.substring(0, ct.indexOf(";")).toUpperCase().trim() : ct.toUpperCase().trim();
								ret.append("\"").append(ct1.replace("\"", "").replace("=", "\" \"")).append("\"");
								if (ecs) ret.append(" ");
							}
							if (f1) {
								ret.append(")");
							}
							ret.append(" NIL NIL");
							String cte = e.headers.getHeader("Content-Transfer-Encoding");
							if (cte == null) cte = "7BIT";
							ret.append(" \"").append(cte.toUpperCase()).append("\"");
							byte[] bbody = e.body.getBytes();
							ret.append(" ").append(e.body.length());
							int lines = 0;
							if (bbody.length > 1) for (int i = 1; i < bbody.length; i++) {
								if (bbody[i - 1] == AvunaHTTPD.crlfb[0] && bbody[i] == AvunaHTTPD.crlfb[1]) {// assumes crlf, however, is always crlf
									lines++;
								}
							}
							ret.append(" ").append(lines);
						}
						ret.append(")");
						trim(ret);
					}else if (s.startsWith("body") || s.equals("rfc822") || s.equals("rfc822.header") || s.equals("rfc822.text")) {
						StringBuilder mhd = new StringBuilder();
						boolean peek = s.startsWith("body.peek") || s.startsWith("rfc822.header");
						if (!peek) {
							if (e.flags.contains("\\Unseen")) e.flags.remove("\\Unseen");
							if (!e.flags.contains("\\Seen")) e.flags.add("\\Seen");
						}
						String s2 = s.startsWith("body") ? s.substring(s.indexOf("[") + 1, s.indexOf("]")) : "";
						if (s.equals("rfc822")) {
							s2 = "";
						}else if (s.equals("rfc822.header")) {
							s2 = "header";
						}else if (s.equals("rfc822.text")) {
							s2 = "text";
						}
						if (s2.equals("")) {
							LinkedHashMap<String, String[]> hdrs = e.headers.getHeaders();
							for (String ss : hdrs.keySet()) {
								String[] values = hdrs.get(ss);
								for (String sss : values) {
									mhd.append(ss).append(": ").append(sss).append(AvunaHTTPD.crlf);
								}
							}
							mhd.append(AvunaHTTPD.crlf);
							if (e.mp != null) {
								mhd.append(new String(e.mp.serialize(host.logger)));
							}else mhd.append(e.body);
						}else {
							String[] kinds = StringFormatter.congealBySurroundings(s2.split(" "), "(", ")");
							for (int i = 0; i < kinds.length; i++) {
								String value = kinds[i];
								if (i != kinds.length - 1 && kinds[i + 1].startsWith("(")) {
									i++;
									value += " " + kinds[i];
								}
								value = value.toLowerCase().trim();
								if (value.equals("header")) {
									LinkedHashMap<String, String[]> hdrs = e.headers.getHeaders();
									for (String ss : hdrs.keySet()) {
										String[] values = hdrs.get(ss);
										for (String sss : values) {
											mhd.append(ss).append(": ").append(sss).append(AvunaHTTPD.crlf);
										}
									}
								}else if (value.equals("text")) {
									if (e.mp != null) {
										mhd.append(new String(e.mp.serialize(host.logger)));
									}else mhd.append(e.body);
								}else if (value.equals("mime")) {
									if (e.headers.hasHeader("content-type")) {
										mhd.append(e.headers.getHeader("content-type"));
									}else {
										mhd.append("text/plain; charset=\"UTF-8\"");
									}
								}else if (value.startsWith("header.fields")) {
									boolean limit = value.contains("(");
									String[] limitList = new String[0];
									if (limit) {
										limitList = value.substring(value.indexOf("(") + 1, value.indexOf(")")).split(" ");
									}
									for (String l : limitList) {
										if (e.headers.hasHeader(l)) {
											for (String v : e.headers.getHeaders(l))
												mhd.append(e.headers.getCurrentCase(l)).append(": ").append(v).append(AvunaHTTPD.crlf);
										}
									}
								}else if (value.startsWith("header.fields.not")) {
									boolean limit = value.contains("(");
									String[] limitList = new String[0];
									if (limit) {
										limitList = value.substring(value.indexOf("(") + 1, value.indexOf(")")).split(" ");
									}
									LinkedHashMap<String, String[]> hdrs = e.headers.getHeaders();
									b: for (String ss : hdrs.keySet()) {
										for (String l : limitList) {
											if (ss.equalsIgnoreCase(l)) {
												continue b;
											}
										}
										String[] values = hdrs.get(ss);
										for (String sss : values) {
											mhd.append(ss).append(": ").append(sss).append(AvunaHTTPD.crlf);
										}
									}
								}else if (e.mp != null) {
									try {
										int part = Integer.parseInt(value);
										part--;
										if (e.mp.mpds.size() > part) {
											MultiPartData mpd = e.mp.mpds.get(part);
											if (mpd != null) {
												mhd.append(new String(mpd.data));
											}
										}
									}catch (NumberFormatException e2) {
										
									}
								}else if (value.equals("1")) {
									mhd.append(e.body);
								}
							}
							mhd.append(AvunaHTTPD.crlf);
						}
						int sub = 0;
						int max = -1;
						String s5 = s.substring(s.indexOf("]") + 1);
						if (s5.startsWith("<")) {
							if (s5.contains(".")) {
								String ss = s5.substring(1, s5.indexOf("."));
								sub = ss.length() > 0 ? Integer.parseInt(ss) : 0;
								String sm = s5.substring(s5.indexOf(".") + 1, s5.length() - 1);
								max = sm.length() > 0 ? Integer.parseInt(sm) : 0;
							}else sub = Integer.parseInt(s5.substring(1, s5.length() - 1));
						}
						String s4 = s3;
						if (peek && s4.toLowerCase().startsWith("body.peek")) {
							s4 = s4.substring(0, 4) + s4.substring(9);
						}
						String r = mhd.toString();
						if (sub > 0) {
							if (sub >= r.length()) r = "";
							else r = r.substring(sub);
						}
						if (max > 0) {
							if (r.length() >= max) r = r.substring(0, max);
						}
						ret.append(s4).append(" {").append(r.length()).append("}").append(AvunaHTTPD.crlf);
						ret.append(r);
						// ret.append(AvunaHTTPD.crlf);
						if (i3 == tps.length - 1) notrim = true;
					}else if (s.equals("envelope")) {
						
					}else if (s.equals("flags")) {
						ret.append("FLAGS (");
						for (String flag : e.flags) {
							ret.append(flag).append(" ");
						}
						trim(ret);
						ret.append(")");
					}else if (s.equals("internaldate")) {
						ret.append("INTERNALDATE ");
						Scanner ed = new Scanner(e.data);
						while (ed.hasNextLine()) {
							String line = ed.nextLine().trim();
							if (line.length() > 0) {
								if (!line.contains(":")) continue;
								String hn = line.substring(0, line.indexOf(":")).trim();
								String hd = line.substring(line.indexOf(":") + 1).trim();
								if (hn.equalsIgnoreCase("date")) {
									ret.append("\"").append(hd).append("\"");
								}
							}else {
								break;
							}
						}
						ed.close();
					}else if (s.equals("rfc822.size")) {
						ret.append("RFC822.SIZE " + e.data.length());
					}else if (s.equals("uid")) {
						ret.append("UID " + e.uid);
					}
					if (!notrim) ret.append(" ");
				}
				if (!notrim) trim(ret);
				ret.append(")");
				focus.writeLine("*", ret.toString());
			}
			focus.writeLine(letters, "OK");
		}else {
			focus.writeLine(letters, "BAD Missing Arguments.");
		}
	}
}
