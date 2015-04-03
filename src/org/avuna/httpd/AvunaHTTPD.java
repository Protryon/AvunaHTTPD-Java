package org.avuna.httpd;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;
import org.avuna.httpd.com.ComClient;
import org.avuna.httpd.com.CommandProcessor;
import org.avuna.httpd.dns.RecordHolder;
import org.avuna.httpd.hosts.Host;
import org.avuna.httpd.hosts.HostCom;
import org.avuna.httpd.hosts.HostDNS;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.hosts.HostRegistry;
import org.avuna.httpd.hosts.Protocol;
import org.avuna.httpd.util.Config;
import org.avuna.httpd.util.ConfigFormat;
import org.avuna.httpd.util.FileManager;
import org.avuna.httpd.util.Logger;

public class AvunaHTTPD {
	public static final String VERSION = "1.1.6";
	public static Config mainConfig, hostsConfig;
	public static final FileManager fileManager = new FileManager();
	public static final HashMap<String, String> extensionToMime = new HashMap<String, String>();
	public static final String crlf = new String(new byte[]{13, 10});
	public static final byte[] crlfb = new byte[]{13, 10};
	
	public static void setupFolders() {
		fileManager.getMainDir().mkdirs();
		fileManager.getLogs().mkdirs();
		for (Host host : hosts.values()) {
			host.setupFolders();
		}
	}
	
	public static void setupScripts() throws IOException {
		String os = System.getProperty("os.name").toLowerCase();
		File us = null;
		try {
			us = new File(AvunaHTTPD.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		}catch (Exception e) {
		}
		if (us == null) return;
		if (os.contains("windows")) {
			File f = fileManager.getBaseFile("run.bat");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("javaw -jar \"" + us.getAbsolutePath() + "\" \"" + fileManager.getBaseFile("main.cfg").getAbsolutePath() + "\"").getBytes());
				fout.flush();
				fout.close();
			}
			f = fileManager.getBaseFile("kill.bat");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("taskkill /f /im javaw").getBytes());
				fout.flush();
				fout.close();
			}
			f = fileManager.getBaseFile("restart.bat");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("kill.bat & run.bat").getBytes());
				fout.flush();
				fout.close();
			}
			f = fileManager.getBaseFile("cmd.bat");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("java -jar \"" + us.getAbsolutePath() + "\" cmd").getBytes());
				fout.flush();
				fout.close();
			}
		}else {
			String ll = new String(new byte[]{0x0A});
			File f = fileManager.getBaseFile("run.sh");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("#!/bin/bash" + ll + ll + "nohup java -jar \"" + us.getAbsolutePath() + "\" \"" + fileManager.getBaseFile("main.cfg").getAbsolutePath() + "\" >& /dev/null &").getBytes());
				fout.flush();
				fout.close();
			}
			f = fileManager.getBaseFile("kill.sh");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("#!/bin/bash" + ll + ll + "pkill -f " + us.getName() + "").getBytes());
				fout.flush();
				fout.close();
			}
			f = fileManager.getBaseFile("restart.sh");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("#!/bin/bash" + ll + ll + "" + new File(us.getParentFile(), "kill.sh").getAbsolutePath() + ll + new File(us.getParentFile(), "run.sh").getAbsolutePath()).getBytes());
				fout.flush();
				fout.close();
			}
			f = fileManager.getBaseFile("cmd.sh");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("#!/bin/bash" + ll + "java -jar \"" + us.getAbsolutePath() + "\" cmd").getBytes());
				fout.flush();
				fout.close();
			}
		}
	}
	
	public static void unpack() {
		try {
			setupScripts();
			String[] unpacks = new String[]{"mime.txt"};
			String os = System.getProperty("os.name").toLowerCase();
			for (String up : unpacks) {
				File mime = fileManager.getBaseFile(up);
				if (!mime.exists()) {
					Logger.log("Unpacking " + up + "...");
					InputStream in = AvunaHTTPD.class.getResourceAsStream("/unpack/" + up);
					int i = 1;
					byte[] buf = new byte[4096];
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					while (i > 0) {
						i = in.read(buf);
						if (i > 0) {
							bout.write(buf, 0, i);
						}
					}
					in.close();
					FileOutputStream fout = new FileOutputStream(mime);
					fout.write(bout.toByteArray());
					fout.flush();
					fout.close();
				}
			}
		}catch (IOException e) {
			Logger.logError(e);
		}
	}
	
	public static void loadUnpacked() {
		try {
			File mime = fileManager.getBaseFile("mime.txt");
			Scanner s = new Scanner(new FileInputStream(mime));
			while (s.hasNextLine()) {
				String line = s.nextLine().trim();
				if (line.length() > 0) {
					String[] ls = line.split(" ");
					if (ls.length > 1) {
						for (int i = 1; i < ls.length; i++) {
							extensionToMime.put(ls[i], ls[0]);
						}
					}
				}
			}
			s.close();
		}catch (IOException e) {
			Logger.logError(e);
		}
	}
	
	public static final HashMap<String, Host> hosts = new HashMap<String, Host>();
	
	public static final RecordHolder records = new RecordHolder();
	
	public static final ArrayList<String> bannedIPs = new ArrayList<String>();
	
	public static void checkPerms(File root) {
		if (!root.exists() && root.isDirectory()) {
			try {
				root.mkdirs();
			}catch (SecurityException e) {
				System.err.println("[WARNING] Cannot read/write to " + root.getAbsolutePath());
			}
		}
		if (!root.canWrite()) {
			System.err.println("[WARNING] Cannot write to " + root.getAbsolutePath());
		}else if (!root.canRead()) {
			System.err.println("[WARNING] Cannot read from " + root.getAbsolutePath());
		}
		if (root.isDirectory()) for (File f : root.listFiles()) {
			checkPerms(f);
		}
	}
	
	public static long lastbipc = 0L;
	
	public static void main(String[] args) {
		try {
			if (args.length >= 1 && args[0].equals("cmd")) {
				String ip = args.length >= 2 ? args[1] : "127.0.0.1";
				int port = args.length >= 3 ? Integer.parseInt(args[2]) : 6049;
				ComClient.run(ip, port);
				return;
			}
			if (System.getProperty("user.name").contains("root")) {
				System.out.println("[NOTIFY] Running as root, will load servers and attempt de-escalate.");
			}
			System.setProperty("line.separator", crlf);
			final boolean unpack = args.length == 1 && args[0].equals("unpack");
			File us = null;
			try {
				us = new File(AvunaHTTPD.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			}catch (Exception e) {
			}
			final File cfg = new File(!unpack && args.length > 0 ? args[0] : (us == null ? (System.getProperty("os.name").toLowerCase().contains("windows") ? "C:\\avuna\\main.cfg" : "/etc/avuna/main.cfg") : (new File(us.getParentFile(), "main.cfg").getAbsolutePath())));
			// checkPerms(cfg.getParentFile());
			mainConfig = new Config("main", cfg, new ConfigFormat() {
				public void format(HashMap<String, Object> map) {
					File dir = null;
					if (!map.containsKey("dir")) {
						map.put("dir", (dir = cfg.getParentFile()).getAbsolutePath());
					}else {
						dir = new File((String)map.get("dir"));
					}
					if (!map.containsKey("hosts")) map.put("hosts", new File(dir, "hosts.cfg").toString());
					if (!map.containsKey("logs")) map.put("logs", new File(dir, "logs").toString());
					if (!map.containsKey("javac")) map.put("javac", "javac");
					if (!map.containsKey("masterChild")) map.put("masterChild", "false");
					if (System.getProperty("os.name").toLowerCase().contains("windows") && map.get("masterChild").equals("true")) {
						Logger.log("You cannot use master child on Windows!");
						map.put("masterChild", "false");
					}
					if (!map.containsKey("masterChildUIDPortStart")) map.put("masterChildUIDPortStart", "6844");
					if (!System.getProperty("os.name").toLowerCase().contains("windows")) if (!map.containsKey("uid")) map.put("uid", unpack ? "6833" : "0");
					if (!System.getProperty("os.name").toLowerCase().contains("windows")) if (!map.containsKey("gid")) map.put("gid", unpack ? "6833" : "0");
				}
			});
			mainConfig.load();
			if (unpack) {
				mainConfig.save();
			}
			HostRegistry.addHost(Protocol.HTTP, HostHTTP.class);
			HostRegistry.addHost(Protocol.COM, HostCom.class);
			HostRegistry.addHost(Protocol.DNS, HostDNS.class);
			HostRegistry.addHost(Protocol.MAIL, HostMail.class);
			HostHTTP.unpack();
			HostCom.unpack();
			HostDNS.unpack();
			HostMail.unpack();
			hostsConfig = new Config("hosts", new File((String)mainConfig.get("hosts")), new ConfigFormat() {
				
				@Override
				public void format(HashMap<String, Object> map) {
					boolean nm = false, nc = false, nd = false;
					if (!map.containsKey("main")) {
						map.put("main", new LinkedHashMap<String, Object>());
						nm = true;
					}
					if (!map.containsKey("com")) {
						map.put("com", new LinkedHashMap<String, Object>());
						nc = true;
					}
					// if (!map.containsKey("dns")) {
					// map.put("dns", new LinkedHashMap<String, Object>());
					// nd = true;
					// }
					for (String key : map.keySet()) {
						HashMap<String, Object> host = (HashMap<String, Object>)map.get(key);
						if (!host.containsKey("enabled")) host.put("enabled", (nc && key.equals("com")) ? "false" : "true");
						if (!host.containsKey("protocol")) host.put("protocol", ((nd && key.equals("dns")) ? "dns" : ((nc && key.equals("com")) ? "com" : "http")));
						Protocol p = Protocol.fromString((String)host.get("protocol"));
						if (p == null) {
							Logger.log("Skipping Host: " + key + " due to invalid protocol!");
							continue;
						}
						if (!host.get("enabled").equals("true")) {
							continue;
						}
						try {
							Host h = (Host)HostRegistry.getHost(p).getConstructors()[0].newInstance(key);
							h.formatConfig(host);
							hosts.put(key, h);
						}catch (Exception e) {
							Logger.logError(e);
							continue;
						}
					}
				}
				
			});
			hostsConfig.load();
			hostsConfig.save();
			setupFolders();
			File lf = new File(fileManager.getLogs(), "" + (System.currentTimeMillis() / 1000L));
			lf.createNewFile();
			Logger.INSTANCE = new Logger(new PrintStream(new FileOutputStream(lf)));
			unpack();
			loadUnpacked();
			Logger.log("Loaded Configs");
			Logger.log("test0");
			for (Host host : hosts.values()) {
				if (host instanceof HostHTTP) {
					((HostHTTP)host).loadBases();
				}
			}
			if (unpack) {
				Logger.log("Unpack complete, terminating.");
				System.exit(0);
			}
			Logger.log("Loading Connection Handling");
			for (Host h : hosts.values()) {
				h.start();
			}
			if (!System.getProperty("os.name").toLowerCase().contains("windows") && System.getProperty("user.name").contains("root") && !mainConfig.get("uid").equals("0")) {
				major:
				while (true) {
					for (Host h : hosts.values()) {
						if (!h.loaded) {
							Thread.sleep(1L);
							continue major;
						}
					}
					break;
				}
				CLib.setuid(Integer.parseInt((String)mainConfig.get("uid")));
				CLib.setgid(Integer.parseInt((String)mainConfig.get("gid")));
				Logger.log("[NOTIFY] De-escalated to uid " + CLib.getuid());
			}else if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
				Logger.log("[NOTIFY] We did NOT de-escalate, currently running as uid " + CLib.getuid());
			}
			for (Host host : hosts.values()) {
				if (host instanceof HostHTTP) {
					((HostHTTP)host).loadCustoms();
				}
			}
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					Logger.log("Softly Terminating!");
					for (Host h : hosts.values()) {
						h.preExit();
					}
					if (AvunaHTTPD.mainConfig != null) {
						AvunaHTTPD.mainConfig.save();
					}
				}
			});
		}catch (Exception e) {
			if (Logger.INSTANCE == null) {
				e.printStackTrace();
			}else {
				Logger.logError(e);
			}
		}
		Scanner scan = new Scanner(System.in);
		while (scan.hasNextLine()) {
			try {
				String command = scan.nextLine();
				CommandProcessor.process(command, System.out, scan);
			}catch (NoSuchElementException fe) {
				break;
			}catch (Exception e) {
				Logger.logError(e);
			}
		}
	}
}
