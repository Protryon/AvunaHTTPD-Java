package org.avuna.httpd.mail.imap.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.IMAPCommand;
import org.avuna.httpd.mail.imap.IMAPWork;
import org.avuna.httpd.mail.mailbox.Email;
import org.avuna.httpd.mail.util.StringFormatter;

public class IMAPCommandFetch extends IMAPCommand {
	
	public IMAPCommandFetch(String comm, int minState, int maxState, HostMail host) {
		super(comm, minState, maxState, host);
	}
	
	@Override
	public void run(IMAPWork focus, String letters, String[] args) throws IOException {
		if (args.length >= 2) {
			String[] seqs = args[0].split(",");
			ArrayList<Email> toFetch = new ArrayList<Email>();
			for (String seq : seqs) {
				if (seq.contains(":")) {
					int i = Integer.parseInt(seq.substring(0, seq.indexOf(":"))) - 1;
					String f = seq.substring(seq.indexOf(":") + 1);
					int f2 = f.equals("*") ? focus.selectedMailbox.emails.size() : Integer.parseInt(f) - 1;
					for (; i < f2; i++) {
						toFetch.add(focus.selectedMailbox.emails.get(i));
					}
				}else {
					if (seq.equals("*")) {
						toFetch.add(focus.selectedMailbox.emails.get(focus.selectedMailbox.emails.size() - 1));
					}else {
						toFetch.add(focus.selectedMailbox.emails.get(Integer.parseInt(seq) - 1));
					}
				}
			}
			String tp = args[1];
			if (tp.startsWith("(")) tp = tp.substring(1, tp.length() - 1);
			String[] tps = tp.split(" ");
			tps = StringFormatter.congealBySurroundings(tps, "[", "]");
			for (Email e : toFetch) {
				if (tps[0].equals("all")) {
					tps = new String[]{"FLAGS", "INTERNALDATE", "RFC822.SIZE", "ENVELOPE"};
				}else if (tps[0].equals("fast")) {
					tps = new String[]{"FLAGS", "INTERNALDATE", "RFC822.SIZE"};
				}else if (tps[0].equals("full")) {
					tps = new String[]{"FLAGS", "INTERNALDATE", "RFC822.SIZE", "ENVELOPE", "BODY"};
				}
				String ret = e.uid + " FETCH (";
				for (String s3 : tps) {
					String s = s3.toLowerCase();
					if (s.equals("body")) {
						
					}else if (s.startsWith("body") || s.equals("rfc822") || s.equals("rfc822.header") || s.equals("rfc822.text")) {
						String mhd = "";
						boolean peek = s.startsWith("body.peek") || s.startsWith("rfc822.header");
						if (!peek) {
							if (e.flags.contains("\\Unseen")) e.flags.remove("\\Unseen");
							if (e.flags.contains("\\Seen")) e.flags.add("\\Seen");
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
							mhd = e.data;
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
									Scanner ed = new Scanner(e.data);
									while (ed.hasNextLine()) {
										String line = ed.nextLine().trim();
										if (line.length() > 0) {
											mhd += line + AvunaHTTPD.crlf;
										}else {
											break;
										}
									}
								}else if (value.equals("text")) {
									Scanner ed = new Scanner(e.data);
									while (ed.hasNextLine()) {
										String line = ed.nextLine().trim();
										if (line.length() > 0) {
											// skip
										}else {
											break;
										}
									}
									while (ed.hasNextLine()) {
										String line = ed.nextLine();
										mhd += line + AvunaHTTPD.crlf;
									}
								}else if (value.equals("mime")) {
									Scanner ed = new Scanner(e.data);
									while (ed.hasNextLine()) {
										String line = ed.nextLine().trim();
										if (line.length() > 0) {
											if (!line.contains(":")) continue;
											String hn = line.substring(0, line.indexOf(":")).trim();
											String hd = line.substring(line.indexOf(":") + 1).trim();
											if (hn.equals("content-type")) {
												mhd += hd;
											}
										}else {
											break;
										}
									}
								}else if (value.startsWith("header.fields")) {
									boolean limit = value.contains("(");
									String[] limitList = new String[0];
									if (limit) {
										limitList = value.substring(value.indexOf("(") + 1, value.indexOf(")")).split(" ");
									}
									List<String> limitList2 = Arrays.asList(limitList);
									limitList = null;
									
									Scanner ed = new Scanner(e.data);
									while (ed.hasNextLine()) {
										String line = ed.nextLine().trim();
										if (line.length() > 0) {
											if (!line.contains(":")) continue;
											String hn = line.substring(0, line.indexOf(":")).trim();
											String hd = line.substring(line.indexOf(":") + 1).trim();
											if (!limit || (limitList2.contains(hn.toLowerCase()))) {
												mhd += hn + ": " + hd + AvunaHTTPD.crlf;
											}
										}else {
											break;
										}
									}
									ed.close();
								}else if (value.startsWith("header.fields.not")) {
									boolean limit = value.contains("(");
									String[] limitList = new String[0];
									if (limit) {
										limitList = value.substring(value.indexOf("(") + 1, value.indexOf(")")).split(" ");
									}
									List<String> limitList2 = Arrays.asList(limitList);
									limitList = null;
									
									Scanner ed = new Scanner(e.data);
									while (ed.hasNextLine()) {
										String line = ed.nextLine().trim();
										if (line.length() > 0) {
											if (!line.contains(":")) continue;
											String hn = line.substring(0, line.indexOf(":")).trim();
											String hd = line.substring(line.indexOf(":") + 1).trim();
											if (!limit || (!limitList2.contains(hn.toLowerCase()))) {
												mhd += hn + ": " + hd + AvunaHTTPD.crlf;
											}
										}else {
											break;
										}
									}
									ed.close();
								}
							}
						}
						int sub = 0;
						String s5 = s.substring(s.indexOf("]"));
						if (s5.startsWith("<")) {
							sub = Integer.parseInt(s5.substring(1, s5.length() - 1));
						}
						String s4 = s3;
						if (peek && s4.toLowerCase().startsWith("body.peek")) {
							s4 = s4.substring(0, 4) + s4.substring(9);
						}
						ret += s4 + " {" + (mhd.length() - 2) + "}" + AvunaHTTPD.crlf;
						ret += (sub > 0 ? mhd.substring(sub) : mhd);
					}else if (s.equals("bodystructure")) {
						
					}else if (s.equals("envelope")) {
						
					}else if (s.equals("flags")) {
						ret += "FLAGS (";
						for (String flag : e.flags) {
							ret += flag + " ";
						}
						ret = ret.trim();
						ret += ")";
					}else if (s.equals("internaldate")) {
						
					}else if (s.equals("rfc822.size")) {
						ret += "RFC822.SIZE " + e.data.length();
					}else if (s.equals("uid")) {
						ret += "UID " + e.uid;
					}
					ret += " ";
				}
				ret = ret.trim();
				ret += ")";
				focus.writeLine(focus, "*", ret);
			}
			focus.writeLine(focus, letters, "OK");
		}else {
			focus.writeLine(focus, letters, "BAD Missing Arguments.");
		}
	}
}
