/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.mailbox.Email;
import org.avuna.httpd.mail.mailbox.EmailAccount;
import org.avuna.httpd.mail.mailbox.Mailbox;

public class HardDriveSync extends Sync {
	
	public HardDriveSync(HostMail host) {
		super(host);
	}
	
	@Override
	public void save(List<EmailAccount> accts) throws IOException {
		File sync = new File(host.getConfig().getNode("folder").getValue());
		File[] pf = sync.listFiles();
		File[][] tf = new File[pf.length][];
		boolean[][] tu = new boolean[pf.length][];
		for (int i = 0; i < pf.length; i++) {
			tf[i] = pf[i].listFiles();
			tu[i] = new boolean[tf[i].length];
		}
		for (EmailAccount acct : accts) {
			String dom = acct.email.substring(acct.email.indexOf("@") + 1);
			File tsync = new File(sync, dom);
			String mb = acct.email.substring(0, acct.email.indexOf("@"));
			File acctf = new File(tsync, mb);
			for (int i = 0; i < tf.length; i++) {
				if (pf[i].getName().equals(dom)) {
					for (int o = 0; o < tf[i].length; o++) {
						if (tf[i][o].getName().equals(mb)) {
							tu[i][o] = true;
						}
					}
					break;
				}
			}
			acctf.mkdirs();
			File acctc = new File(acctf, "cfg");
			acctc.createNewFile();
			DataOutputStream fout2 = new DataOutputStream(new FileOutputStream(acctc));
			fout2.writeInt(acct.password.length());
			fout2.write(acct.password.getBytes());
			fout2.flush();
			fout2.close();
			for (Mailbox m : acct.mailboxes) {
				File mf = new File(acctf, m.name);
				mf.mkdirs();
				synchronized (m.emails) {
					for (int i = 0; i < m.emails.length; i++) {
						Email e = m.emails[i];
						File f = new File(mf, (i + 1) + ".eml");
						if (e == null) {
							if (f.exists()) {
								f.delete();
							}
							continue;
						}
						DataOutputStream fout = new DataOutputStream(new FileOutputStream(f));
						fout.writeInt(e.from.length());
						fout.write(e.from.getBytes());
						fout.writeInt(e.to.size());
						for (String to : e.to) {
							fout.writeInt(to.length());
							fout.write(to.getBytes());
						}
						fout.writeInt(e.flags.size());
						for (String flag : e.flags) {
							fout.writeInt(flag.length());
							fout.write(flag.getBytes());
						}
						fout.writeInt(e.data.length());
						fout.write(e.data.getBytes());
						fout.flush();
						fout.close();
					}
				}
			}
		}
		for (int i = 0; i < tu.length; i++) {
			for (int o = 0; o < tu[i].length; o++) {
				if (!tu[i][o]) {
					delete(tf[i][o]);
				}
			}
		}
	}
	
	private static void delete(File f) {
		if (f.isDirectory()) {
			for (File sf : f.listFiles()) {
				delete(sf);
			}
			f.delete();
		}else {
			f.delete();
		}
	}
	
	public void load(List<EmailAccount> accts) {
		File sync = new File(host.getConfig().getNode("folder").getValue());
		if (!sync.isDirectory()) return;
		for (File domf : sync.listFiles()) {
			for (File acctf : domf.listFiles()) {
				if (acctf.isDirectory()) {
					try {
						String email = acctf.getName() + "@" + domf.getName();
						DataInputStream fin2 = new DataInputStream(new FileInputStream(new File(acctf, "cfg")));
						byte[] pwba = new byte[fin2.readInt()];
						fin2.readFully(pwba);
						fin2.close();
						String password = new String(pwba);
						EmailAccount acct = new EmailAccount(email, password);
						for (File mf : acctf.listFiles()) {
							if (mf.isDirectory()) {
								Mailbox m = acct.getMailbox(mf.getName());
								if (m == null) {
									acct.mailboxes.add(m = new Mailbox(acct, mf.getName()));
								}
								for (File eml : mf.listFiles()) {
									try {
										if (eml.isFile()) {
											int uid = -1;
											try {
												uid = Integer.parseInt(eml.getName().substring(0, eml.getName().indexOf(".")));
											}catch (NumberFormatException e) {
												continue;
											}
											DataInputStream fin = new DataInputStream(new FileInputStream(eml));
											byte[] fba = new byte[fin.readInt()];
											fin.readFully(fba);
											String from = new String(fba);
											int tl = fin.readInt();
											String[] toa = new String[tl];
											for (int i = 0; i < tl; i++) {
												byte[] tba = new byte[fin.readInt()];
												fin.readFully(tba);
												toa[i] = new String(tba);
											}
											int fl = fin.readInt();
											String[] fla = new String[fl];
											for (int i = 0; i < fl; i++) {
												byte[] flba = new byte[fin.readInt()];
												fin.readFully(flba);
												fla[i] = new String(flba);
											}
											byte[] dba = new byte[fin.readInt()];
											fin.readFully(dba);
											String data = new String(dba);
											Email e = new Email(host, data, uid, from);
											for (String flag : fla) {
												if (!e.flags.contains(flag)) e.flags.add(flag);
											}
											for (String to : toa) {
												e.to.add(to);
											}
											synchronized (m.emails) {
												if (m.emails.length < e.uid) {
													Email[] ne = new Email[e.uid];
													System.arraycopy(m.emails, 0, ne, 0, m.emails.length);
													ne[e.uid - 1] = e;
													m.emails = ne;
												}
											}
											fin.close();
										}
									}catch (Exception e) {
										host.logger.logError(e);
										host.logger.log("Error loading email: " + eml.getAbsolutePath());
									}
								}
							}
						}
						host.accounts.add(acct);
					}catch (IOException e) {
						host.logger.logError(e);
						host.logger.log("Error loading account: " + acctf.getAbsolutePath());
					}
				}
			}
		}
	}
}
