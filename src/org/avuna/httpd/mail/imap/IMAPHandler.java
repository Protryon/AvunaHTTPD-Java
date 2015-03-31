package org.avuna.httpd.mail.imap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import javax.xml.bind.DatatypeConverter;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.mailbox.Email;
import org.avuna.httpd.mail.mailbox.EmailAccount;
import org.avuna.httpd.mail.mailbox.Mailbox;
import org.avuna.httpd.mail.util.StringFormatter;

public class IMAPHandler {
	public final ArrayList<IMAPCommand> commands = new ArrayList<IMAPCommand>();
	
	public IMAPHandler(final HostMail host) {
		commands.add(new IMAPCommand("capability", 0, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				focus.writeLine(focus, "*", "CAPABILITY IMAP4rev1 AUTH=PLAIN LOGINDISABLED");
				focus.writeLine(focus, letters, "OK Capability completed.");
			}
			
		});
		commands.add(new IMAPCommand("logout", 0, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				focus.writeLine(focus, letters, "OK " + ((String)host.getConfig().get("domain")).split(",")[0] + " terminating connection.");
				focus.s.close();
			}
			
		});
		commands.add(new IMAPCommand("noop", 0, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				focus.writeLine(focus, letters, "OK");
			}
			
		});
		commands.add(new IMAPCommand("authenticate", 0, 0) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				if (args.length >= 1 && args[0].equalsIgnoreCase("plain")) {
					focus.writeLine(focus, "", "+");
					focus.state = 1;
					focus.authMethod = "plain" + letters;
				}else {
					focus.writeLine(focus, letters, "BAD No type.");
				}
			}
			
		});
		commands.add(new IMAPCommand("", 1, 1) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				String up = new String(DatatypeConverter.parseBase64Binary(letters)).substring(1);
				String username = up.substring(0, up.indexOf(new String(new byte[]{0})));
				String password = up.substring(username.length() + 1);
				String letters2 = focus.authMethod.substring(5);
				System.out.println(username + ":" + password);
				EmailAccount us = null;
				for (EmailAccount e : host.accounts) {
					if (e.email.equals(username) && e.password.equals(password)) {
						us = e;
						break;
					}
				}
				if (us != null) {
					focus.writeLine(focus, letters2, "OK");
					focus.authUser = us;
					focus.state = 2;
				}else {
					focus.writeLine(focus, letters2, "NO Authenticate Failed.");
					focus.state = 0;
				}
			}
			
		});
		
		commands.add(new IMAPCommand("select", 2, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				if (args.length >= 1) {
					String ms = args[0];
					if (ms.startsWith("\"")) {
						ms = ms.substring(1);
					}
					if (ms.endsWith("\"")) {
						ms = ms.substring(0, ms.length() - 1);
					}
					Mailbox m = focus.authUser.getMailbox(ms);
					if (m != null) {
						if (focus.selectedMailbox != null) {
							focus.writeLine(focus, "*", "OK [CLOSED] Previous mailbox closed.");
						}
						focus.selectedMailbox = m;
						focus.state = 3;
						focus.writeLine(focus, "*", "FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft)");
						focus.writeLine(focus, "*", "OK [PERMANENTFLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft \\*)] Flags permitted.");
						focus.writeLine(focus, "*", m.emails.size() + " EXISTS");
						int recent = 0;
						for (Email e : m.emails) {
							if (e.flags.contains("\\Recent")) recent++;
						}
						int unseen = 0;
						for (Email e : m.emails) {
							if (!e.flags.contains("\\Seen")) unseen++;
						}
						focus.writeLine(focus, "*", recent + " RECENT");
						focus.writeLine(focus, "*", "OK [UNSEEN " + unseen + "] Unseen messages");
						focus.writeLine(focus, "*", "OK [UIDVALIDITY " + Integer.MAX_VALUE + "] UIDs valid");
						focus.writeLine(focus, "*", "OK [UIDNEXT " + (m.emails.size() + 1) + "] Predicted next UID");
						focus.writeLine(focus, "*", "OK [HIGHESTMODSEQ 1] Highest");
						focus.writeLine(focus, letters, "OK [READ-WRITE] Select completed.");
					}else {
						focus.writeLine(focus, letters, "NO Invalid mailbox.");
					}
				}else {
					focus.writeLine(focus, letters, "BAD No mailbox.");
				}
			}
			
		});
		
		commands.add(new IMAPCommand("examine", 2, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				if (args.length >= 1) {
					String ms = args[0];
					if (ms.startsWith("\"")) {
						ms = ms.substring(1);
					}
					if (ms.endsWith("\"")) {
						ms = ms.substring(0, ms.length() - 1);
					}
					Mailbox m = focus.authUser.getMailbox(ms);
					if (m != null) {
						if (focus.selectedMailbox != null) {
							focus.writeLine(focus, "*", "OK [CLOSED] Previous mailbox closed.");
						}
						focus.selectedMailbox = m;
						focus.isExamine = true;
						focus.state = 3;
						focus.writeLine(focus, "*", "FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft)");
						focus.writeLine(focus, "*", "OK [PERMANENTFLAGS ()] Read-only mailbox.");
						focus.writeLine(focus, "*", m.emails.size() + " EXISTS");
						int recent = 0;
						for (Email e : m.emails) {
							if (e.flags.contains("\\Recent")) recent++;
						}
						focus.writeLine(focus, "*", recent + " RECENT");
						focus.writeLine(focus, "*", "OK [UIDVALIDITY " + Integer.MAX_VALUE + "] UIDs valid");
						focus.writeLine(focus, "*", "OK [UIDNEXT " + (m.emails.size() + 1) + "] Predicted next UID");
						focus.writeLine(focus, "*", "OK [HIGHESTMODSEQ 1] Highest");
						focus.writeLine(focus, letters, "OK [READ-ONLY] Select completed.");
					}else {
						focus.writeLine(focus, letters, "NO Invalid mailbox.");
					}
				}else {
					focus.writeLine(focus, letters, "BAD No mailbox.");
				}
			}
			
		});
		
		commands.add(new IMAPCommand("create", 2, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				if (args.length >= 1) {
					String ms = args[0];
					if (ms.startsWith("\"")) {
						ms = ms.substring(1);
					}
					if (ms.endsWith("\"")) {
						ms = ms.substring(0, ms.length() - 1);
					}
					if (focus.authUser.getMailbox(ms) != null) {
						focus.writeLine(focus, letters, "NO Mailbox Exists.");
					}else {
						focus.authUser.mailboxes.add(new Mailbox(focus.authUser, ms));
						focus.writeLine(focus, letters, "OK Mailbox created.");
					}
				}else {
					focus.writeLine(focus, letters, "BAD No mailbox.");
				}
			}
			
		});
		
		commands.add(new IMAPCommand("delete", 2, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				if (args.length >= 1) {
					String ms = args[0];
					if (ms.startsWith("\"")) {
						ms = ms.substring(1);
					}
					if (ms.endsWith("\"")) {
						ms = ms.substring(0, ms.length() - 1);
					}
					Mailbox m = focus.authUser.getMailbox(ms);
					if (m == null || m.name.equals("INBOX") || m.name.equals("Trash")) {
						focus.writeLine(focus, letters, "NO Invalid Mailbox.");
					}else {
						focus.authUser.mailboxes.remove(m);
						focus.writeLine(focus, letters, "OK Mailbox deleted.");
					}
				}else {
					focus.writeLine(focus, letters, "BAD No mailbox.");
				}
			}
			
		});
		
		commands.add(new IMAPCommand("rename", 2, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				if (args.length >= 2) {
					String ms = args[0];
					if (ms.startsWith("\"")) {
						ms = ms.substring(1);
					}
					if (ms.endsWith("\"")) {
						ms = ms.substring(0, ms.length() - 1);
					}
					String nn = args[1];
					if (nn.startsWith("\"")) {
						nn = nn.substring(1);
					}
					if (nn.endsWith("\"")) {
						nn = nn.substring(0, ms.length() - 1);
					}
					Mailbox m = focus.authUser.getMailbox(ms);
					if (m == null || m.name.equals("INBOX") || m.name.equals("Trash")) {
						focus.writeLine(focus, letters, "NO Invalid Mailbox.");
					}else {
						m.name = nn;
						focus.writeLine(focus, letters, "OK Mailbox renamed.");
					}
				}else {
					focus.writeLine(focus, letters, "BAD No mailbox.");
				}
			}
			
		});
		
		commands.add(new IMAPCommand("subscribe", 2, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				if (args.length >= 1) {
					String ms = args[0];
					if (ms.startsWith("\"")) {
						ms = ms.substring(1);
					}
					if (ms.endsWith("\"")) {
						ms = ms.substring(0, ms.length() - 1);
					}
					Mailbox m = focus.authUser.getMailbox(ms);
					if (m == null) {
						focus.writeLine(focus, letters, "NO Invalid Mailbox.");
					}else {
						m.subscribed = true;
						focus.writeLine(focus, letters, "OK Mailbox subscribed.");
					}
				}else {
					focus.writeLine(focus, letters, "BAD No mailbox.");
				}
			}
			
		});
		
		commands.add(new IMAPCommand("unsubscribe", 2, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				if (args.length >= 1) {
					String ms = args[0];
					if (ms.startsWith("\"")) {
						ms = ms.substring(1);
					}
					if (ms.endsWith("\"")) {
						ms = ms.substring(0, ms.length() - 1);
					}
					Mailbox m = focus.authUser.getMailbox(ms);
					if (m == null) {
						focus.writeLine(focus, letters, "NO Invalid Mailbox.");
					}else {
						m.subscribed = false;
						focus.writeLine(focus, letters, "OK Mailbox unsubscribed.");
					}
				}else {
					focus.writeLine(focus, letters, "BAD No mailbox.");
				}
			}
			
		});
		
		commands.add(new IMAPCommand("list", 2, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				if (args.length >= 2) {
					String rn = args[0];
					if (rn.startsWith("\"")) {
						rn = rn.substring(1);
					}
					if (rn.endsWith("\"")) {
						rn = rn.substring(0, rn.length() - 1);
					}
					String mn = args[1];
					if (mn.startsWith("\"")) {
						mn = mn.substring(1);
					}
					if (mn.endsWith("\"")) {
						mn = mn.substring(0, mn.length() - 1);
					}
					if (!mn.equals("*")) {
						Mailbox m = mn.length() == 0 && focus.selectedMailbox != null ? focus.selectedMailbox : focus.authUser.getMailbox(mn);
						if (m == null) {
							focus.writeLine(focus, letters, "NO Invalid Mailbox.");
						}else {
							focus.writeLine(focus, "*", "LIST (\\HasNoChildren) \"" + rn + "\" \"" + m.name + "\"");
							focus.writeLine(focus, letters, "OK Mailbox list.");
						}
					}else {
						for (Mailbox m : focus.authUser.mailboxes) {
							if (!m.subscribed) {
								m = null;
							}
							if (m != null) {
								focus.writeLine(focus, "*", "LIST (\\HasNoChildren) \"" + rn + "\" \"" + m.name + "\"");
							}
						}
						focus.writeLine(focus, letters, "OK Mailbox list.");
					}
				}else {
					focus.writeLine(focus, letters, "BAD No mailbox.");
				}
			}
			
		});
		
		commands.add(new IMAPCommand("lsub", 2, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				if (args.length >= 2) {
					String rn = args[0];
					if (rn.startsWith("\"")) {
						rn = rn.substring(1);
					}
					if (rn.endsWith("\"")) {
						rn = rn.substring(0, rn.length() - 1);
					}
					if (rn.equals("")) rn = ".";
					String mn = args[1];
					if (mn.startsWith("\"")) {
						mn = mn.substring(1);
					}
					if (mn.endsWith("\"")) {
						mn = mn.substring(0, mn.length() - 1);
					}
					
					if (!mn.equals("*")) {
						Mailbox m = mn.length() == 0 && focus.selectedMailbox != null ? focus.selectedMailbox : focus.authUser.getMailbox(mn);
						if (m != null && !m.subscribed) {
							m = null;
						}
						if (m == null) {
							focus.writeLine(focus, letters, "NO Invalid Mailbox.");
						}else {
							focus.writeLine(focus, "*", "LSUB (\\HasNoChildren) \"" + rn + "\" \"" + m.name + "\"");
							focus.writeLine(focus, letters, "OK Mailbox list.");
						}
					}else {
						for (Mailbox m : focus.authUser.mailboxes) {
							if (!m.subscribed) {
								m = null;
							}
							if (m != null) {
								focus.writeLine(focus, "*", "LSUB (\\HasNoChildren) \"" + rn + "\" \"" + m.name + "\"");
							}
						}
						focus.writeLine(focus, letters, "OK Mailbox list.");
					}
					
				}else {
					focus.writeLine(focus, letters, "BAD No mailbox.");
				}
			}
			
		});
		
		commands.add(new IMAPCommand("status", 2, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				if (args.length >= 1) {
					String ms = args[0];
					if (ms.startsWith("\"")) {
						ms = ms.substring(1);
					}
					if (ms.endsWith("\"")) {
						ms = ms.substring(0, ms.length() - 1);
					}
					Mailbox m = focus.authUser.getMailbox(ms);
					if (m == null) {
						focus.writeLine(focus, letters, "NO Invalid Mailbox.");
					}else {
						int recent = 0;
						for (Email e : m.emails) {
							if (e.flags.contains("\\Recent")) recent++;
						}
						int unseen = 0;
						for (Email e : m.emails) {
							if (!e.flags.contains("\\Seen")) unseen++;
						}
						focus.writeLine(focus, "*", "STATUS " + m.name + " (MESSAGES " + m.emails.size() + " RECENT " + recent + " UIDNEXT " + (m.emails.size() + 1) + " UIDVALIDITY " + Integer.MAX_VALUE + " UNSEEN unseen)");
						focus.writeLine(focus, letters, "OK Status.");
					}
				}else {
					focus.writeLine(focus, letters, "BAD No mailbox.");
				}
			}
			
		});
		
		commands.add(new IMAPCommand("append", 2, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				if (args.length >= 2) {
					focus.writeLine(focus, letters, "BAD Not Implemented.");
				}else {
					focus.writeLine(focus, letters, "BAD No mailbox.");
				}
			}
			
		});
		
		commands.add(new IMAPCommand("check", 3, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				focus.writeLine(focus, letters, "OK check.");
			}
			
		});
		
		commands.add(new IMAPCommand("close", 3, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				for (int i = 0; i < focus.selectedMailbox.emails.size(); i++) {
					if (focus.selectedMailbox.emails.get(i).flags.contains("\\Deleted")) {
						focus.selectedMailbox.emails.remove(i);
						i--;
					}
				}
				focus.state = 2;
				focus.selectedMailbox = null;
				focus.writeLine(focus, letters, "OK close.");
			}
			
		});
		
		commands.add(new IMAPCommand("expunge", 3, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				for (int i = 0; i < focus.selectedMailbox.emails.size(); i++) {
					if (focus.selectedMailbox.emails.get(i).flags.contains("\\Deleted")) {
						focus.selectedMailbox.emails.remove(i);
						focus.writeLine(focus, "*", (i + 1) + " EXPUNGE");
						i--;
					}
				}
				focus.writeLine(focus, letters, "OK expunge.");
			}
			
		});
		
		commands.add(new IMAPCommand("search", 3, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				if (args.length >= 2) {
					args = new String[]{args[1]};
				}
				if (args.length >= 1) {
					focus.writeLine(focus, "*", "SEARCH");
				}
				focus.writeLine(focus, letters, "OK Not yet implemented.");
			}
			
		});
		
		final IMAPCommand fetch;
		
		commands.add(fetch = new IMAPCommand("fetch", 3, 100) {
			
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
			
		});
		
		final IMAPCommand store;
		
		commands.add(store = new IMAPCommand("store", 3, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				if (args.length >= 3) {
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
					String fc = args[1].toLowerCase();
					String[] flags = args[2].substring(1, args[2].length() - 1).split(" ");
					for (Email e : toFetch) {
						if (fc.startsWith("+")) {
							for (String flag : flags) {
								if (flag.equals("\\Seen")) {
									e.flags.remove("\\Unseen");
								}else if (flag.equals("\\Unseen")) {
									e.flags.remove("\\Seen");
								}
								e.flags.add(flag);
							}
						}else if (fc.startsWith("-")) {
							for (String flag : flags) {
								if (e.flags.contains(flag)) {
									e.flags.remove(flag);
								}
							}
						}else {
							boolean recent = e.flags.contains("\\Recent");
							e.flags.clear();
							if (recent) e.flags.add("\\Recent");
							for (String flag : flags) {
								e.flags.add(flag);
							}
						}
						if (!fc.toLowerCase().endsWith(".silent")) {
							String ret = e.uid + " FETCH (FLAGS (";
							for (String flag : e.flags) {
								ret += flag + " ";
							}
							ret = ret.trim();
							ret += "))";
							focus.writeLine(focus, "*", ret);
						}
					}
					focus.writeLine(focus, letters, "OK");
				}else {
					focus.writeLine(focus, letters, "BAD Missing Arguments.");
				}
			}
			
		});
		
		commands.add(new IMAPCommand("copy", 3, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				focus.writeLine(focus, letters, "NO Not yet implemented.");
			}
			
		});
		
		commands.add(new IMAPCommand("uid", 3, 100) {
			
			@Override
			public void run(IMAPWork focus, String letters, String[] args) throws IOException {
				if (args.length >= 1) {
					if (args[0].toLowerCase().equals("fetch")) {
						String[] nargs = new String[args.length - 1];
						for (int i = 0; i < nargs.length; i++) {
							nargs[i] = args[i + 1];
						}
						if (nargs.length >= 2 && nargs[1].startsWith("(") && !nargs[1].contains("UID")) {
							nargs[1] = "(UID " + nargs[1].substring(1);
						}
						fetch.run(focus, letters, nargs);
					}else if (args[0].toLowerCase().equals("store")) {
						String[] nargs = new String[args.length - 1];
						for (int i = 0; i < nargs.length; i++) {
							nargs[i] = args[i + 1];
						}
						store.run(focus, letters, nargs);
					}
				}else {
					focus.writeLine(focus, letters, "BAD Missing Arguments.");
				}
			}
			
		});
		
	}
}
