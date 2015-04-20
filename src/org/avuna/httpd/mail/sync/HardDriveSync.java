package org.avuna.httpd.mail.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.mailbox.Email;
import org.avuna.httpd.mail.mailbox.EmailAccount;
import org.avuna.httpd.mail.mailbox.Mailbox;

public class HardDriveSync extends Sync {
	
	public HardDriveSync(HostMail host) {
		super(host);
	}
	
	@Override
	public void save(ArrayList<EmailAccount> accts) throws IOException {
		File sync = new File(host.getConfig().getNode("folder").getValue());
		for (EmailAccount acct : accts) {
			File tsync = new File(sync, acct.email.substring(acct.email.indexOf("@") + 1));
			File acctf = new File(tsync, acct.email.substring(0, acct.email.indexOf("@")));
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
				for (Email e : m.emails) {
					DataOutputStream fout = new DataOutputStream(new FileOutputStream(new File(mf, e.uid + ".eml")));
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
	
	public void load(ArrayList<EmailAccount> accts) throws IOException {
		File sync = new File(host.getConfig().getNode("folder").getValue());
		if (!sync.isDirectory()) return;
		for (File domf : sync.listFiles()) {
			for (File acctf : domf.listFiles()) {
				if (acctf.isDirectory()) {
					String email = acctf.getName() + "@" + domf.getName();
					DataInputStream fin2 = new DataInputStream(new FileInputStream(new File(acctf, "cfg")));
					byte[] pwba = new byte[fin2.readInt()];
					fin2.readFully(pwba);
					String password = new String(pwba);
					EmailAccount acct = new EmailAccount(email, password);
					for (File mf : acctf.listFiles()) {
						if (mf.isDirectory()) {
							Mailbox m = acct.getMailbox(mf.getName());
							if (m == null) {
								acct.mailboxes.add(m = new Mailbox(acct, mf.getName()));
							}
							for (File eml : mf.listFiles()) {
								if (eml.isFile()) {
									int uid = Integer.parseInt(eml.getName().substring(0, eml.getName().indexOf(".")));
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
									Email e = new Email(data, uid, from);
									for (String flag : fla) {
										e.flags.add(flag);
									}
									for (String to : toa) {
										e.to.add(to);
									}
									m.emails.add(e);
								}
							}
						}
					}
					host.accounts.add(acct);
				}
			}
		}
	}
}
