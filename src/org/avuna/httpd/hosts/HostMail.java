package org.avuna.httpd.hosts;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import javax.net.ssl.SSLServerSocketFactory;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.mail.imap.IMAPHandler;
import org.avuna.httpd.mail.imap.IMAPWork;
import org.avuna.httpd.mail.imap.ThreadAcceptIMAP;
import org.avuna.httpd.mail.imap.ThreadWorkerIMAP;
import org.avuna.httpd.mail.mailbox.EmailAccount;
import org.avuna.httpd.mail.smtp.SMTPHandler;
import org.avuna.httpd.mail.smtp.SMTPWork;
import org.avuna.httpd.mail.smtp.ThreadAcceptSMTP;
import org.avuna.httpd.mail.smtp.ThreadWorkerSMTP;
import org.avuna.httpd.mail.sync.HardDriveSync;
import org.avuna.httpd.mail.sync.Sync;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.Logger;

public class HostMail extends Host {
	
	private int tac, twc, mc;
	public SMTPHandler smtphandler = new SMTPHandler(this);
	public IMAPHandler imaphandler = new IMAPHandler(this);
	
	public HostMail(String name) {
		super(name, Protocol.MAIL);
	}
	
	public Sync sync;
	
	public void clearWorkSMTP() {
		workQueueSMTP.clear();
	}
	
	public ArrayBlockingQueue<SMTPWork> workQueueSMTP;
	public final ArrayList<ThreadWorkerSMTP> workersSMTP = new ArrayList<ThreadWorkerSMTP>();
	
	public void addWorkSMTP(Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		workQueueSMTP.add(new SMTPWork(s, in, out, ssl));
	}
	
	public int getQueueSizeSMTP() {
		return workQueueSMTP.size();
	}
	
	public void clearWorkIMAP() {
		workQueueIMAP.clear();
	}
	
	public ArrayBlockingQueue<IMAPWork> workQueueIMAP;
	public final ArrayList<ThreadWorkerIMAP> workersIMAP = new ArrayList<ThreadWorkerIMAP>();
	
	public void addWorkIMAP(Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		workQueueIMAP.add(new IMAPWork(s, in, out, ssl));
	}
	
	public int getQueueSizeIMAP() {
		return workQueueIMAP.size();
	}
	
	public void setupFolders() {
		new File(getConfig().getNode("folder").getValue()).mkdirs();
	}
	
	public static void unpack() {
		AvunaHTTPD.fileManager.getBaseFile("mail").mkdirs();
	}
	
	public final ArrayList<EmailAccount> accounts = new ArrayList<EmailAccount>();
	
	public void formatConfig(ConfigNode map) {
		if (!map.containsNode("smtp-port")) map.insertNode("smtp-port", "25", "mail delivery port");
		if (!map.containsNode("smtp-mua-port")) map.insertNode("smtp-mua-port", "587", "email client port");
		if (!map.containsNode("smtp-tls-port")) map.insertNode("smtp-tls-port", "465", "TLS port for SMTP");
		if (!map.containsNode("imap-port")) map.insertNode("imap-port", "143", "IMAP port");
		if (!map.containsNode("imap-tls-port")) map.insertNode("imap-tls-port", "993", "IMAPS port");
		if (!map.containsNode("ip")) map.insertNode("ip", "0.0.0.0", "bind ip");
		if (!map.containsNode("ssl")) map.insertNode("ssl", "configure to enable imaps/smtps/starttls");
		ConfigNode ssl = map.getNode("ssl");
		if (!ssl.containsNode("enabled")) ssl.insertNode("enabled", "false");
		if (!ssl.containsNode("keyFile")) ssl.insertNode("keyFile", AvunaHTTPD.fileManager.getBaseFile("ssl/keyFile").toString());
		if (!ssl.containsNode("keystorePassword")) ssl.insertNode("keystorePassword", "password");
		if (!ssl.containsNode("keyPassword")) ssl.insertNode("keyPassword", "password");
		if (!map.containsNode("domain")) map.insertNode("domain", "example.com,example.org", "domains to accept mail from");
		if (!map.containsNode("folder")) map.insertNode("folder", AvunaHTTPD.fileManager.getBaseFile("mail").toString(), "mail storage folder");
		if (!map.containsNode("acceptThreadCount")) map.insertNode("acceptThreadCount", "2", "accept thread count");
		if (!map.containsNode("workerThreadCount")) map.insertNode("workerThreadCount", "8", "worker thread count");
		if (!map.containsNode("maxConnections")) map.insertNode("maxConnections", "-1", "max connections per port");
		tac = Integer.parseInt(map.getNode("acceptThreadCount").getValue());
		twc = Integer.parseInt(map.getNode("workerThreadCount").getValue());
		mc = Integer.parseInt(map.getNode("maxConnections").getValue());
	}
	
	public void registerAccount(String email, String password) {
		accounts.add(new EmailAccount(email, password));
	}
	
	public void run() {
		try {
			ConfigNode cfg = getConfig();
			sync = new HardDriveSync(this);
			sync.load(accounts);
			// registerAccount("test@example.com", "test123");
			ConfigNode ssl = cfg.getNode("ssl");
			String ip = cfg.getNode("ip").getValue();
			ServerSocket smtp = makeServer(ip, Integer.parseInt(cfg.getNode("smtp-port").getValue()), false, null);
			ServerSocket smtpmua = makeServer(ip, Integer.parseInt(cfg.getNode("smtp-mua-port").getValue()), false, null);
			ServerSocket imap = makeServer(ip, Integer.parseInt(cfg.getNode("imap-port").getValue()), false, null);
			ServerSocket smtps = null, imaps = null;
			if (ssl.getNode("enabled").getValue().equals("true")) {
				sslContext = makeSSLContext(new File(ssl.getNode("keyFile").getValue()), ssl.getNode("keyPassword").getValue(), ssl.getNode("keystorePassword").getValue());
				SSLServerSocketFactory sssf = sslContext.getServerSocketFactory();
				smtps = makeServer(ip, Integer.parseInt(cfg.getNode("smtp-tls-port").getValue()), true, sssf);
				imaps = makeServer(ip, Integer.parseInt(cfg.getNode("imap-tls-port").getValue()), true, sssf);
			}
			workQueueSMTP = new ArrayBlockingQueue<SMTPWork>(mc < 0 ? 1000000 : mc);
			workQueueIMAP = new ArrayBlockingQueue<IMAPWork>(mc < 0 ? 1000000 : mc);
			for (int i = 0; i < twc; i++) {
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
			for (int i = 0; i < twc; i++) {
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
						Logger.logError(e);
					}
				}
			});
		}catch (Exception e) {
			Logger.logError(e);
			Logger.log("Closing " + name + "/" + protocol.name + " Server on " + getConfig().getNode("ip").getValue() + ":" + getConfig().getNode("port").getValue());
		}finally {
			loaded = true;
		}
	}
	
	@Override
	public void setup(ServerSocket s) {
		
	}
}
