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
			String seq = args[0];
			ArrayList<Email> toFetch = new ArrayList<Email>();
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
			String[] tps = args[1].substring(1, args[1].length() - 1).split(" ");
			tps = StringFormatter.congealBySurroundings(tps, "[", "]");
			for (Email e : toFetch) {
				String ret = e.uid + " FETCH (";
				for (String s3 : tps) {
					String s = s3.toLowerCase();
					if (s.equals("uid")) {
						ret += "UID " + e.uid;
					}else if (s.equals("rfc822.size")) {
						ret += "RFC822.SIZE " + e.data.length();
					}else if (s.equals("flags")) {
						ret += "FLAGS (";
						for (String flag : e.flags) {
							ret += flag + " ";
						}
						ret = ret.trim();
						ret += ")";
					}else if (s.startsWith("body")) {
						String mhd = "";
						if (s.contains("[") && s.contains("]") && !(s.indexOf("]") == (s.indexOf("[") + 1))) {
							String s2 = s.substring(s.indexOf("[") + 1, s.indexOf("]"));
							String[] kinds = StringFormatter.congealBySurroundings(s2.split(" "), "(", ")");
							for (int i = 0; i < kinds.length; i++) {
								String value = kinds[i];
								if (i != kinds.length - 1 && kinds[i + 1].startsWith("(")) {
									i++;
									value += " " + kinds[i];
								}
								value = value.toLowerCase().trim();
								if (value.startsWith("header.fields")) {
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
								}
							}
						}else {
							mhd = e.data;
						}
						String s4 = s3;
						if (s4.toLowerCase().startsWith("body.peek")) {
							s4 = "BODY" + s4.substring(9);
						}else {
							if (e.flags.contains("\\Unseen")) e.flags.remove("\\Unseen");
							if (e.flags.contains("\\Seen")) e.flags.add("\\Seen");
						}
						ret += s4 + " {" + (mhd.length() - 2) + "}" + AvunaHTTPD.crlf;
						ret += mhd;
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
