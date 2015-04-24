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
import java.util.NoSuchElementException;
import java.util.Scanner;
import org.avuna.httpd.CLib.bap;
import org.avuna.httpd.com.ComClient;
import org.avuna.httpd.com.CommandProcessor;
import org.avuna.httpd.dns.RecordHolder;
import org.avuna.httpd.hosts.Host;
import org.avuna.httpd.hosts.HostCom;
import org.avuna.httpd.hosts.HostDNS;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.hosts.HostHTTPM;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.hosts.HostRegistry;
import org.avuna.httpd.hosts.Protocol;
import org.avuna.httpd.util.Config;
import org.avuna.httpd.util.ConfigFormat;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.FileManager;
import org.avuna.httpd.util.Logger;

public class AvunaHTTPD {
	public static final String VERSION = "1.1.9";
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
	
	public static boolean isSymlink(File f) {
		if (windows) return false;
		byte[] rb = f.getAbsolutePath().getBytes();
		CLib.bap bap = new CLib.bap(rb.length);
		System.arraycopy(rb, 0, bap.array, 0, rb.length);
		return isSymlink(bap);
	}
	
	private static boolean isSymlink(bap f) {
		bap bap = new bap(255);
		int length = CLib.INSTANCE.readlink(f, bap, 255);
		return length >= 0;
	}
	
	public static void setPerms(File root, int uid, int gid, int chmod) {
		// Logger.log("Setting " + root.getAbsolutePath() + " to " + uid + ":" + gid + " chmod " + chmod);
		byte[] rb = root.getAbsolutePath().getBytes();
		CLib.bap bap = new CLib.bap(rb.length);
		System.arraycopy(rb, 0, bap.array, 0, rb.length);
		if (isSymlink(bap)) return;
		int ch = CLib.INSTANCE.chmod(bap, chmod);
		// Logger.log("lchmod returned: " + ch + (ch == -1 ? " error code: " + Native.getLastError() : ""));
		CLib.INSTANCE.lchown(bap, uid, gid);
	}
	
	public static void setPerms(File root, int uid, int gid) {
		setPerms(root, uid, gid, false);
	}
	
	// should run as root
	private static void setPerms(File root, int uid, int gid, boolean recursed) {
		CLib.INSTANCE.umask(0000);
		setPerms(root, uid, gid, 0700);
		for (File f : root.listFiles()) {
			if (f.isDirectory()) {
				setPerms(f, uid, gid, true);
			}else {
				if (uid == 0 && gid == 0) {
					setPerms(f, 0, 0, 0700);
				}else {
					String rn = f.getName();
					if (!recursed && (rn.equals("avuna.jar") || rn.equals("main.cfg"))) {
						setPerms(f, 0, gid, 0640);
					}else if (!recursed && (rn.equals("kill.sh") || rn.equals("run.sh") || rn.equals("restart.sh") || rn.equals("cmd.sh"))) {
						setPerms(f, 0, 0, 0700);
					}else {
						setPerms(f, uid, gid, 0700);
					}
				}
			}
		}
		CLib.INSTANCE.umask(0077);
	}
	
	public static long lastbipc = 0L;
	public static final boolean windows = System.getProperty("os.name").toLowerCase().contains("windows");
	
	public static void main(String[] args) {
		try {
			if (args.length >= 1) {
				if (args[0].equals("cmd")) {
					String ip = args.length >= 2 ? args[1] : "127.0.0.1";
					int port = args.length >= 3 ? Integer.parseInt(args[2]) : 6049;
					ComClient.run(ip, port);
					return;
				}else if (args[0].equals("ucmd")) {
					if (windows) {
						System.out.println("MUST be on a unix system!");
						return;
					}
					if (args.length != 2) {
						System.out.println("MUST specify unix socket file!");
						return;
					}
					ComClient.runUnix(args[1]);
					return;
				}
				
			}
			if (System.getProperty("user.name").contains("root")) {
				System.out.println("[NOTIFY] Running as root, will load servers and attempt de-escalate, if configured.");
			}
			System.setProperty("line.separator", crlf);
			final boolean unpack = args.length == 1 && args[0].equals("unpack");
			File us = null;
			try {
				us = new File(AvunaHTTPD.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			}catch (Exception e) {
			}
			final File cfg = new File(!unpack && args.length > 0 ? args[0] : (us == null ? (System.getProperty("os.name").toLowerCase().contains("windows") ? "C:\\avuna\\main.cfg" : "/etc/avuna/main.cfg") : (new File(us.getParentFile(), "main.cfg").getAbsolutePath())));
			mainConfig = new Config("main", cfg, new ConfigFormat() {
				public void format(ConfigNode map) {
					File dir = null;
					if (!map.containsNode("dir")) {
						map.insertNode("dir", (dir = cfg.getParentFile()).getAbsolutePath());
					}else {
						dir = new File(map.getNode("dir").getValue());
					}
					if (!map.containsNode("hosts")) map.insertNode("hosts", new File(dir, "hosts.cfg").toString());
					if (!map.containsNode("logs")) map.insertNode("logs", new File(dir, "logs").toString());
					if (!map.containsNode("javac")) map.insertNode("javac", "javac");
					if (!windows && !map.containsNode("uid")) map.insertNode("uid", unpack ? "6833" : "0");
					if (!windows && !map.containsNode("gid")) map.insertNode("gid", unpack ? "6833" : "0");
					if (!windows && !map.containsNode("safeMode")) map.insertNode("safeMode", "true");
				}
			});
			mainConfig.load();
			if (unpack) {
				mainConfig.save();
			}
			HostRegistry.addHost(Protocol.HTTP, HostHTTP.class);
			HostRegistry.addHost(Protocol.HTTPM, HostHTTPM.class);
			HostRegistry.addHost(Protocol.COM, HostCom.class);
			HostRegistry.addHost(Protocol.DNS, HostDNS.class);
			HostRegistry.addHost(Protocol.MAIL, HostMail.class);
			HostHTTP.unpack();
			HostCom.unpack();
			HostDNS.unpack();
			HostMail.unpack();
			hostsConfig = new Config("hosts", new File(mainConfig.getNode("hosts").getValue()), new ConfigFormat() {
				
				@Override
				public void format(ConfigNode map) {
					boolean nm = false, nc = false, nd = false;
					if (!map.containsNode("main")) {
						map.insertNode("main");
						nm = true;
					}
					if (!map.containsNode("com")) {
						map.insertNode("com");
						nc = true;
					}
					// if (!map.containsKey("dns")) {
					// map.put("dns", new LinkedHashMap<String, Object>());
					// nd = true;
					// }
					for (String key : map.getSubnodes()) {
						ConfigNode host = map.getNode(key);
						if (!host.containsNode("enabled")) host.insertNode("enabled", (nc && key.equals("com")) ? "false" : "true");
						if (!host.containsNode("protocol")) host.insertNode("protocol", ((nd && key.equals("dns")) ? "dns" : ((nc && key.equals("com")) ? "com" : "http")));
						Protocol p = Protocol.fromString(host.getNode("protocol").getValue());
						if (p == null) {
							Logger.log("Skipping Host: " + key + " due to invalid protocol!");
							continue;
						}
						if (!host.getNode("enabled").getValue().equals("true")) {
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
			if (!windows && mainConfig.getNode("safeMode").getValue().equals("true")) {
				setPerms(cfg.getParentFile(), Integer.parseInt(mainConfig.getNode("uid").getValue()), Integer.parseInt(mainConfig.getNode("gid").getValue()));
			}
			if (!windows && CLib.INSTANCE.getuid() == 0 && !mainConfig.getNode("uid").getValue().equals("0")) {
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
				CLib.INSTANCE.setuid(Integer.parseInt(mainConfig.getNode("uid").getValue()));
				CLib.INSTANCE.setgid(Integer.parseInt(mainConfig.getNode("gid").getValue()));
				Logger.log("[NOTIFY] De-escalated to uid " + CLib.INSTANCE.getuid());
			}else if (!windows) {
				Logger.log("[NOTIFY] We did NOT de-escalate, currently running as uid " + CLib.INSTANCE.getuid());
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
