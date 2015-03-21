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
import org.avuna.httpd.hosts.HostRegistry;
import org.avuna.httpd.hosts.Protocol;
import org.avuna.httpd.plugins.PatchBus;
import org.avuna.httpd.plugins.base.BaseLoader;
import org.avuna.httpd.util.Config;
import org.avuna.httpd.util.ConfigFormat;
import org.avuna.httpd.util.FileManager;
import org.avuna.httpd.util.Logger;

public class AvunaHTTPD {
	public static final String VERSION = "1.0.8";
	public static Config mainConfig, hostsConfig;
	private static Config dnsConfig;
	public static final FileManager fileManager = new FileManager();
	public static final PatchBus patchBus = new PatchBus();
	public static final HashMap<String, String> extensionToMime = new HashMap<String, String>();
	public static final String crlf = new String(new byte[]{13, 10});
	public static final byte[] crlfb = new byte[]{13, 10};
	
	public static void setupFolders() {
		fileManager.getMainDir().mkdirs();
		fileManager.getLogs().mkdirs();
		fileManager.getPlugins().mkdirs();
		for (Host host : hosts.values()) {
			host.setupFolders();
		}
		patchBus.setupFolders();
	}
	
	public static void setupScripts() throws IOException {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("windows")) {
			File f = fileManager.getBaseFile("run.bat");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("javaw -jar \"" + fileManager.getBaseFile("jws.jar").getAbsolutePath() + "\" \"" + fileManager.getBaseFile("main.cfg").getAbsolutePath() + "\"").getBytes());
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
				fout.write(("java -jar \"" + fileManager.getBaseFile("jws.jar").getAbsolutePath() + "\" cmd").getBytes());
				fout.flush();
				fout.close();
			}
		}else {
			File f = fileManager.getBaseFile("run.sh");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("nohup java -jar \"" + fileManager.getBaseFile("jws.jar").getAbsolutePath() + "\" \"" + fileManager.getBaseFile("main.cfg").getAbsolutePath() + "\" >& /dev/null &").getBytes());
				fout.flush();
				fout.close();
			}
			f = fileManager.getBaseFile("kill.sh");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("pkill -f jws.jar").getBytes());
				fout.flush();
				fout.close();
			}
			f = fileManager.getBaseFile("restart.sh");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("sh kill.sh & sh run.sh").getBytes());
				fout.flush();
				fout.close();
			}
			f = fileManager.getBaseFile("cmd.sh");
			if (!f.exists()) {
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(("java -jar \"" + fileManager.getBaseFile("jws.jar").getAbsolutePath() + "\" cmd").getBytes());
				fout.flush();
				fout.close();
			}
		}
	}
	
	public static void unpack() {
		try {
			setupScripts();
			String[] unpacks = new String[]{"mime.txt", "run.sh", "kill.sh", "restart.sh", "cmd.sh", "run.bat", "kill.bat", "restart.bat", "cmd.bat"};
			String os = System.getProperty("os.name").toLowerCase();
			for (String up : unpacks) {
				if (up.endsWith(".sh") && !(os.contains("nux") || os.contains("nix") || os.contains("mac"))) {
					continue;
				}
				if (up.endsWith(".bat") && !os.contains("windows")) {
					continue;
				}
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
				Logger.log("[FATAL] Cannot read/write to " + root.getAbsolutePath());
				System.exit(0);
			}
		}
		if (!root.canWrite()) {
			Logger.log("[FATAL] Cannot write to " + root.getAbsolutePath());
			System.exit(0);
		}else if (!root.canRead()) {
			Logger.log("[FATAL] Cannot read from " + root.getAbsolutePath());
			System.exit(0);
		}
		if (root.isDirectory()) for (File f : root.listFiles()) {
			checkPerms(f);
		}
	}
	
	public static void main(String[] args) {
		try {
			if (args.length >= 1 && args[0].equals("cmd")) {
				String ip = args.length >= 2 ? args[1] : "127.0.0.1";
				int port = args.length >= 3 ? Integer.parseInt(args[2]) : 6049;
				ComClient.run(ip, port);
				return;
			}
			System.setProperty("line.separator", crlf);
			boolean unpack = args.length == 1 && args[0].equals("unpack");
			final File cfg = new File(!unpack && args.length > 0 ? args[0] : (System.getProperty("os.name").toLowerCase().contains("windows") ? "C:\\jws\\main.cfg" : "/etc/jws/main.cfg"));
			checkPerms(cfg.getParentFile());
			mainConfig = new Config("main", cfg, new ConfigFormat() {
				public void format(HashMap<String, Object> map) {
					File dir = null;
					if (!map.containsKey("dir")) {
						map.put("dir", (dir = cfg.getParentFile()).getAbsolutePath());
					}else {
						dir = new File((String)map.get("dir"));
					}
					if (!map.containsKey("hosts")) map.put("hosts", new File(dir, "hosts.cfg").toString());
					if (!map.containsKey("plugins")) map.put("plugins", new File(dir, "plugins").toString());
					if (!map.containsKey("logs")) map.put("logs", new File(dir, "logs").toString());
					if (!map.containsKey("javac")) map.put("javac", "javac");
				}
			});
			mainConfig.load();
			mainConfig.save();
			HostRegistry.addHost(Protocol.HTTP, HostHTTP.class);
			HostRegistry.addHost(Protocol.COM, HostCom.class);
			HostRegistry.addHost(Protocol.DNS, HostDNS.class);
			hostsConfig = new Config("hosts", new File((String)mainConfig.get("hosts")), new ConfigFormat() {
				
				@Override
				public void format(HashMap<String, Object> map) {
					boolean nm = false, nc = false;
					if (!map.containsKey("main")) {
						map.put("main", new LinkedHashMap<String, Object>());
						nm = true;
					}
					if (!map.containsKey("com")) {
						map.put("com", new LinkedHashMap<String, Object>());
						nc = true;
					}
					for (String key : map.keySet()) {
						HashMap<String, Object> host = (HashMap<String, Object>)map.get(key);
						if (!host.containsKey("enabled")) host.put("enabled", "true");
						if (!host.containsKey("protocol")) host.put("protocol", ((nc && key.equals("com")) ? "com" : "http"));
						if (!host.containsKey("port")) host.put("port", ((nc && key.equals("com")) ? "6049" : "80"));
						if (!host.containsKey("ip")) host.put("ip", "0.0.0.0");
						if (!host.containsKey("ssl")) host.put("ssl", new LinkedHashMap<String, Object>());
						HashMap<String, Object> ssl = (HashMap<String, Object>)host.get("ssl");
						if (!ssl.containsKey("enabled")) ssl.put("enabled", "false");
						if (!ssl.containsKey("keyFile")) ssl.put("keyFile", fileManager.getBaseFile("ssl/keyFile").toString());
						if (!ssl.containsKey("keystorePassword")) ssl.put("keystorePassword", "password");
						if (!ssl.containsKey("keyPassword")) ssl.put("keyPassword", "password");
						Protocol p = Protocol.fromString((String)host.get("protocol"));
						if (p == null) {
							Logger.log("Skipping Host: " + key + " due to invalid protocol!");
							continue;
						}
						if (!host.get("enabled").equals("true")) {
							continue;
						}
						try {
							Host h = (Host)HostRegistry.getHost(p).getConstructors()[0].newInstance(key, (String)host.get("ip"), Integer.parseInt((String)host.get("port")), ssl.get("enabled").equals("true"), new File((String)ssl.get("keyFile")), (String)ssl.get("keyPassword"), (String)ssl.get("keystorePassword"));
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
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					Logger.log("Softly Terminating!");
					AvunaHTTPD.patchBus.preExit();
					if (AvunaHTTPD.mainConfig != null) {
						AvunaHTTPD.mainConfig.save();
					}
				}
			});
			setupFolders();
			File lf = new File(fileManager.getLogs(), "" + (System.currentTimeMillis() / 1000L));
			lf.createNewFile();
			Logger.INSTANCE = new Logger(new PrintStream(new FileOutputStream(lf)));
			unpack();
			loadUnpacked();
			Logger.log("Loaded Configs");
			if (unpack) {
				return;
			}
			Logger.log("Loading Plugins");
			BaseLoader.loadBases();
			Logger.log("Loading Connection Handling");
			for (Host h : hosts.values()) {
				h.start();
			}
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
