package org.avuna.httpd.mail.sync;

import java.io.IOException;
import java.util.ArrayList;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.mailbox.EmailAccount;

public abstract class Sync {
	public final HostMail host;
	
	public Sync(HostMail host) {
		this.host = host;
	}
	
	public abstract void save(ArrayList<EmailAccount> accts) throws IOException;
	
	public abstract void load(ArrayList<EmailAccount> accts) throws IOException;
}
