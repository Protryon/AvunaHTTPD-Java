package org.avuna.httpd.hosts;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ArrayBlockingQueue;
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
		new File((String)getConfig().get("folder")).mkdirs();
	}
	
	public static void unpack() {
		AvunaHTTPD.fileManager.getBaseFile("mail").mkdirs();
	}
	
	public final ArrayList<EmailAccount> accounts = new ArrayList<EmailAccount>();
	
	public void formatConfig(HashMap<String, Object> map) {
		if (!map.containsKey("smtp-port")) map.put("smtp-port", "25");
		if (!map.containsKey("smtp-tls-port")) map.put("smtp-tls-port", "465");
		if (!map.containsKey("imap-port")) map.put("imap-port", "143");
		if (!map.containsKey("imap-tls-port")) map.put("imap-tls-port", "993");
		if (!map.containsKey("ip")) map.put("ip", "0.0.0.0");
		if (!map.containsKey("ssl")) map.put("ssl", new LinkedHashMap<String, Object>());
		HashMap<String, Object> ssl = (HashMap<String, Object>)map.get("ssl");
		if (!ssl.containsKey("enabled")) ssl.put("enabled", "false");
		if (!ssl.containsKey("keyFile")) ssl.put("keyFile", AvunaHTTPD.fileManager.getBaseFile("ssl/keyFile").toString());
		if (!ssl.containsKey("keystorePassword")) ssl.put("keystorePassword", "password");
		if (!ssl.containsKey("keyPassword")) ssl.put("keyPassword", "password");
		if (!map.containsKey("domain")) map.put("domain", "example.com,example.org");
		if (!map.containsKey("folder")) map.put("folder", AvunaHTTPD.fileManager.getBaseFile("mail"));
		if (!map.containsKey("acceptThreadCount")) map.put("acceptThreadCount", "2");
		if (!map.containsKey("workerThreadCount")) map.put("workerThreadCount", "8");
		if (!map.containsKey("maxConnections")) map.put("maxConnections", "-1");
		tac = Integer.parseInt((String)map.get("acceptThreadCount"));
		twc = Integer.parseInt((String)map.get("workerThreadCount"));
		mc = Integer.parseInt((String)map.get("maxConnections"));
	}
	
	public void registerAccount(String email, String password) {
		accounts.add(new EmailAccount(email, password));
	}
	
	public void run() {
		try {
			LinkedHashMap<String, Object> cfg = getConfig();
			sync = new HardDriveSync(this);
			sync.load(accounts);
			// registerAccount("test@example.com", "test123");
			LinkedHashMap<String, Object> ssl = (LinkedHashMap<String, Object>)cfg.get("ssl");
			ServerSocket smtp = makeServer((String)cfg.get("ip"), Integer.parseInt((String)cfg.get("smtp-port")), false, new File((String)ssl.get("keyFile")), (String)ssl.get("keyPassword"), (String)ssl.get("keystorePassword"));
			ServerSocket imap = makeServer((String)cfg.get("ip"), Integer.parseInt((String)cfg.get("imap-port")), false, new File((String)ssl.get("keyFile")), (String)ssl.get("keyPassword"), (String)ssl.get("keystorePassword"));
			ServerSocket smtps = null, imaps = null;
			if (ssl.get("enabled").equals("true")) {
				smtps = makeServer((String)cfg.get("ip"), Integer.parseInt((String)cfg.get("smtp-tls-port")), true, new File((String)ssl.get("keyFile")), (String)ssl.get("keyPassword"), (String)ssl.get("keystorePassword"));
				imaps = makeServer((String)cfg.get("ip"), Integer.parseInt((String)cfg.get("imap-tls-port")), true, new File((String)ssl.get("keyFile")), (String)ssl.get("keyPassword"), (String)ssl.get("keystorePassword"));
			}
			workQueueSMTP = new ArrayBlockingQueue<SMTPWork>(mc < 0 ? 1000000 : mc);
			workQueueIMAP = new ArrayBlockingQueue<IMAPWork>(mc < 0 ? 1000000 : mc);
			for (int i = 0; i < twc; i++) {
				ThreadWorkerSMTP tws = new ThreadWorkerSMTP(this);
				workersSMTP.add(tws);
				tws.start();
			}
			for (int i = 0; i < tac; i++) {
				new ThreadAcceptSMTP(this, smtp, mc).start();
				if (smtps != null) {
					new ThreadAcceptSMTP(this, smtps, mc).start();
				}
			}
			for (int i = 0; i < twc; i++) {
				ThreadWorkerIMAP tws = new ThreadWorkerIMAP(this);
				workersIMAP.add(tws);
				tws.start();
			}
			for (int i = 0; i < tac; i++) {
				new ThreadAcceptIMAP(this, imap, mc).start();
				if (imaps != null) {
					new ThreadAcceptIMAP(this, imaps, mc).start();
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
			Logger.log("Closing " + name + "/" + protocol.name + " Server on " + (String)getConfig().get("ip") + ":" + (String)getConfig().get("port"));
		}finally {
			loaded = true;
		}
	}
	
	@Override
	public void setup(ServerSocket s) {
		
	}
}
