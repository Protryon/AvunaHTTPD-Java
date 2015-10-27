/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.hosts;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.mail.imap.IMAPHandler;
import org.avuna.httpd.mail.imap.IMAPPacketReceiver;
import org.avuna.httpd.mail.imap.IMAPWork;
import org.avuna.httpd.mail.imap.ThreadAcceptIMAP;
import org.avuna.httpd.mail.imap.ThreadWorkerIMAP;
import org.avuna.httpd.mail.imap.ThreadWorkerIMAPUNIO;
import org.avuna.httpd.mail.mailbox.EmailAccount;
import org.avuna.httpd.mail.smtp.SMTPHandler;
import org.avuna.httpd.mail.smtp.SMTPPacketReceiver;
import org.avuna.httpd.mail.smtp.SMTPWork;
import org.avuna.httpd.mail.smtp.ThreadAcceptSMTP;
import org.avuna.httpd.mail.smtp.ThreadWorkerSMTP;
import org.avuna.httpd.mail.smtp.ThreadWorkerSMTPUNIO;
import org.avuna.httpd.mail.sync.HardDriveSync;
import org.avuna.httpd.mail.sync.Sync;
import org.avuna.httpd.util.CLib;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.unio.PacketReceiver;
import org.avuna.httpd.util.unio.UNIOServerSocket;
import org.avuna.httpd.util.unio.UNIOSocket;

public class HostMail extends Host {
	
	private int tac, twc, mc, se;
	public SMTPHandler smtphandler = new SMTPHandler(this);
	public IMAPHandler imaphandler = new IMAPHandler(this);
	
	public HostMail(String name) {
		super(name, Protocol.MAIL);
	}
	
	public Sync sync;
	
	public final ArrayList<ThreadWorkerSMTP> workersSMTP = new ArrayList<ThreadWorkerSMTP>();
	
	public void addWorkSMTP(Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		if (unio()) {
			UNIOSocket us = (UNIOSocket) s;
			((ThreadWorkerSMTPUNIO) SMTPconns.get(ci)).poller.addSocket(us);
			ci++;
			if (ci == IMAPconns.size()) ci = 0;
		}
		SMTPworks.add(new SMTPWork(this, s, in, out, ssl));
	}
	
	public List<IMAPWork> IMAPworks = Collections.synchronizedList(new ArrayList<IMAPWork>());
	public ArrayList<ThreadWorkerIMAPUNIO> IMAPconns = new ArrayList<ThreadWorkerIMAPUNIO>();
	private volatile int ci = 0;
	public final ArrayList<ThreadWorkerIMAP> workersIMAP = new ArrayList<ThreadWorkerIMAP>();
	
	public void addWorkIMAP(Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		if (unio()) {
			UNIOSocket us = (UNIOSocket) s;
			((ThreadWorkerIMAPUNIO) IMAPconns.get(ci)).poller.addSocket(us);
			ci++;
			if (ci == IMAPconns.size()) ci = 0;
		}
		IMAPworks.add(new IMAPWork(this, s, in, out, ssl));
	}
	
	public IMAPWork getIMAPWork() {
		if (unio()) return null;
		synchronized (IMAPworks) {
			for (int i = 0; i < IMAPworks.size(); i++) {
				IMAPWork work = IMAPworks.get(i);
				if (work.inUse) continue;
				try {
					if (work.s.isClosed()) {
						work.close();
						i--;
						continue;
					}else {
						if (work.in.available() > 0) {
							work.inUse = true;
							return work;
						}
					}
				}catch (IOException e) {
					work.inUse = true;
					return work;
				}
			}
		}
		return null;
	}
	
	public List<SMTPWork> SMTPworks = Collections.synchronizedList(new ArrayList<SMTPWork>());
	public ArrayList<ThreadWorkerSMTPUNIO> SMTPconns = new ArrayList<ThreadWorkerSMTPUNIO>();
	
	public SMTPWork getSMTPWork() {
		if (unio()) return null;
		synchronized (SMTPworks) {
			for (int i = 0; i < SMTPworks.size(); i++) {
				SMTPWork work = SMTPworks.get(i);
				if (work.inUse) continue;
				try {
					if (work.s.isClosed()) {
						work.close();
						i--;
						continue;
					}else {
						if (work.in.available() > 0) {
							work.inUse = true;
							return work;
						}
					}
				}catch (IOException e) {
					work.inUse = true;
					return work;
				}
			}
		}
		return null;
	}
	
	public void setupFolders() {
		new File(getConfig().getNode("folder").getValue()).mkdirs();
	}
	
	public static void unpack() {
		AvunaHTTPD.fileManager.getBaseFile("mail").mkdirs();
	}
	
	public final List<EmailAccount> accounts = Collections.synchronizedList(new ArrayList<EmailAccount>());
	
	public void formatConfig(ConfigNode map) {
		if (!map.containsNode("sync-interval")) map.insertNode("sync-interval", "60", "default is to sync to hard drive every 60 seconds");
		if (!map.containsNode("smtp-port")) map.insertNode("smtp-port", "25", "mail delivery port");
		if (!map.containsNode("smtp-mua-port")) map.insertNode("smtp-mua-port", "587", "email client port");
		if (!map.containsNode("smtp-tls-port")) map.insertNode("smtp-tls-port", "465", "TLS port for SMTP");
		if (!map.containsNode("imap-port")) map.insertNode("imap-port", "143", "IMAP port");
		if (!map.containsNode("imap-tls-port")) map.insertNode("imap-tls-port", "993", "IMAPS port");
		if (!map.containsNode("ip")) map.insertNode("ip", "0.0.0.0", "bind ip");
		if (!map.containsNode("ssl")) map.insertNode("ssl", null, "configure to enable imaps/smtps/starttls");
		ConfigNode ssl = map.getNode("ssl");
		if (!ssl.containsNode("enabled")) ssl.insertNode("enabled", "false");
		if (CLib.failed || CLib.hasGNUTLS() == 0) {
			if (!ssl.containsNode("keyFile")) ssl.insertNode("keyFile", AvunaHTTPD.fileManager.getBaseFile("ssl/keyFile").toString());
			if (!ssl.containsNode("keystorePassword")) ssl.insertNode("keystorePassword", "password");
			if (!ssl.containsNode("keyPassword")) ssl.insertNode("keyPassword", "password");
		}else {
			if (!ssl.containsNode("cert")) ssl.insertNode("cert", AvunaHTTPD.fileManager.getBaseFile("ssl/ssl.cert").getAbsolutePath());
			if (!ssl.containsNode("privateKey")) ssl.insertNode("privateKey", AvunaHTTPD.fileManager.getBaseFile("ssl/ssl.pem").getAbsolutePath());
			if (!ssl.containsNode("ca")) ssl.insertNode("ca", AvunaHTTPD.fileManager.getBaseFile("ssl/ca.cert").getAbsolutePath());
		}
		if (!map.containsNode("domain")) map.insertNode("domain", "example.com,example.org", "domains to accept mail from");
		if (!map.containsNode("folder")) map.insertNode("folder", AvunaHTTPD.fileManager.getBaseFile("mail").toString(), "mail storage folder");
		if (!map.containsNode("acceptThreadCount")) map.insertNode("acceptThreadCount", "2", "accept thread count");
		if (!map.containsNode("workerThreadCount")) map.insertNode("workerThreadCount", "8", "worker thread count");
		if (!map.containsNode("maxConnections")) map.insertNode("maxConnections", "-1", "max connections per port");
		tac = Integer.parseInt(map.getNode("acceptThreadCount").getValue());
		twc = Integer.parseInt(map.getNode("workerThreadCount").getValue());
		mc = Integer.parseInt(map.getNode("maxConnections").getValue());
		se = Integer.parseInt(map.getNode("sync-interval").getValue());
	}
	
	public void registerAccount(String email, String password) {
		accounts.add(new EmailAccount(email, password));
	}
	
	public void changePassword(String email, String newPassword) {
		for (EmailAccount eml : accounts) {
			if (eml.email.equalsIgnoreCase(email)) {
				eml.password = newPassword;
				break;
			}
		}
	}
	
	public boolean enableUNIO() {
		return true;
	}
	
	public PacketReceiver makeReceiver(UNIOServerSocket server) {
		return (server == smtp || server == smtpmua || server == smtps) ? new SMTPPacketReceiver() : new IMAPPacketReceiver();
	}
	
	ServerSocket smtp = null, smtpmua = null, imap = null;
	public ServerSocket smtps = null;
	public ServerSocket imaps = null;
	
	public void run() {
		try {
			ConfigNode cfg = getConfig();
			sync = new HardDriveSync(this);
			sync.load(accounts);
			// registerAccount("test@example.com", "test123");
			ConfigNode ssl = cfg.getNode("ssl");
			String ip = cfg.getNode("ip").getValue();
			smtp = makeServer(ip, Integer.parseInt(cfg.getNode("smtp-port").getValue()), false);
			smtpmua = makeServer(ip, Integer.parseInt(cfg.getNode("smtp-mua-port").getValue()), false);
			imap = makeServer(ip, Integer.parseInt(cfg.getNode("imap-port").getValue()), false);
			this.ssl = !(ssl == null || !ssl.getNode("enabled").getValue().equals("true"));
			nssl = !CLib.failed && CLib.hasGNUTLS() == 1;
			if (this.ssl) {
				if (nssl) {
					this.certFile = new File(ssl.getValue("cert")).getAbsolutePath();
					this.pkFile = new File(ssl.getValue("privateKey")).getAbsolutePath();
					this.caFile = new File(ssl.getValue("ca")).getAbsolutePath();
				}else {
					sslContext = makeSSLContext(new File(ssl.getNode("keyFile").getValue()), ssl.getNode("keyPassword").getValue(), ssl.getNode("keystorePassword").getValue());
				}
			}
			if (this.ssl) {
				if (!nssl) {
					sslContext = makeSSLContext(new File(ssl.getNode("keyFile").getValue()), ssl.getNode("keyPassword").getValue(), ssl.getNode("keystorePassword").getValue());
				}
				smtps = makeServer(ip, Integer.parseInt(cfg.getNode("smtp-tls-port").getValue()), true);
				imaps = makeServer(ip, Integer.parseInt(cfg.getNode("imap-tls-port").getValue()), true);
			}
			if (unio()) for (int i = 0; i < twc; i++) {
				ThreadWorkerSMTPUNIO tws = new ThreadWorkerSMTPUNIO(this);
				addTerm(tws);
				workersSMTP.add(tws);
				tws.start();
			}
			else for (int i = 0; i < twc; i++) {
				ThreadWorkerSMTP tws = new ThreadWorkerSMTP(this);
				addTerm(tws);
				workersSMTP.add(tws);
				tws.start();
			}
			for (int i = 0; i < tac; i++) {
				ThreadAcceptSMTP tas1 = new ThreadAcceptSMTP(this, smtp, mc);
				tas1.start();
				ThreadAcceptSMTP tas2 = new ThreadAcceptSMTP(this, smtpmua, mc);
				tas2.start();
				if (smtps != null) {
					ThreadAcceptSMTP tas3 = new ThreadAcceptSMTP(this, smtps, mc);
					tas3.start();
				}
			}
			if (unio()) for (int i = 0; i < twc; i++) {
				ThreadWorkerIMAPUNIO tws = new ThreadWorkerIMAPUNIO(this);
				addTerm(tws);
				workersIMAP.add(tws);
				tws.start();
			}
			else for (int i = 0; i < twc; i++) {
				ThreadWorkerIMAP tws = new ThreadWorkerIMAP(this);
				addTerm(tws);
				workersIMAP.add(tws);
				tws.start();
			}
			for (int i = 0; i < tac; i++) {
				ThreadAcceptIMAP tai1 = new ThreadAcceptIMAP(this, imap, mc);
				tai1.start();
				if (imaps != null) {
					ThreadAcceptIMAP tai2 = new ThreadAcceptIMAP(this, imaps, mc);
					tai2.start();
				}
			}
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						sync.save(accounts);
					}catch (IOException e) {
						logger.logError(e);
					}
				}
			});
		}catch (Exception e) {
			logger.logError(e);
			logger.log("Closing " + name + "/" + protocol.name + " Server on " + getConfig().getNode("ip").getValue());
		}finally {
			loaded = true;
		}
		while (loaded)
			try {
				sync.save(accounts);
				Thread.sleep(se * 1000L);
			}catch (Exception e) {
				logger.logError(e);
			}
	}
	
	@Override
	public void setup(ServerSocket s) {
	
	}
}
