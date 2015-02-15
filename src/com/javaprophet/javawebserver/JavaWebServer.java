package com.javaprophet.javawebserver;

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
import com.javaprophet.javawebserver.dns.RecordHolder;
import com.javaprophet.javawebserver.dns.TCPServer;
import com.javaprophet.javawebserver.dns.ThreadDNSWorker;
import com.javaprophet.javawebserver.dns.UDPServer;
import com.javaprophet.javawebserver.hosts.Host;
import com.javaprophet.javawebserver.hosts.VHost;
import com.javaprophet.javawebserver.networking.ThreadWorker;
import com.javaprophet.javawebserver.networking.command.ComClient;
import com.javaprophet.javawebserver.networking.command.ComServer;
import com.javaprophet.javawebserver.networking.command.CommandProcessor;
import com.javaprophet.javawebserver.plugins.PatchBus;
import com.javaprophet.javawebserver.plugins.base.BaseLoader;
import com.javaprophet.javawebserver.util.Config;
import com.javaprophet.javawebserver.util.ConfigFormat;
import com.javaprophet.javawebserver.util.FileManager;
import com.javaprophet.javawebserver.util.Logger;

public class JavaWebServer {
	public static final String VERSION = "1.0";
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
	
	public static void unpack() {
		try {
			String[] unpacks = new String[]{"mime.txt", "run.sh", "kill.sh", "restart.sh", "cmd.sh", "run.bat", "kill.bat", "restart.bat", "cmd.bat"};
			for (String up : unpacks) {
				if (up.endsWith(".sh") && !System.getProperty("os.name").contains("nux")) {
					continue;
				}
				if (up.endsWith(".bat") && !System.getProperty("os.name").toLowerCase().contains("windows")) {
					continue;
				}
				File mime = fileManager.getBaseFile(up);
				if (!mime.exists()) {
					Logger.log("Unpacking " + up + "...");
					InputStream in = JavaWebServer.class.getResourceAsStream("/unpack/" + up);
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
	
	public static void main(String[] args) {
		try {
			if (args.length >= 1 && args[0].equals("cmd")) {
				String ip = args.length >= 2 ? args[1] : "127.0.0.1";
				int port = args.length >= 3 ? Integer.parseInt(args[2]) : 6049;
				ComClient.run(ip, port);
				return;
			}
			System.setProperty("line.separator", crlf);
			final File cfg = new File(args.length > 0 ? args[0] : (System.getProperty("os.name").toLowerCase().contains("windows") ? "C:\\jws\\main.cfg" : "/etc/jws/main.cfg"));
			mainConfig = new Config("main", cfg, new ConfigFormat() {
				public void format(HashMap<String, Object> map) {
					if (!map.containsKey("version")) map.put("version", JavaWebServer.VERSION);
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
					if (!map.containsKey("connlimit")) map.put("connlimit", "-1");
					if (!map.containsKey("workerThreadCount")) map.put("workerThreadCount", "" + (Runtime.getRuntime().availableProcessors() * 3));
					if (!map.containsKey("errorpages")) map.put("errorpages", new HashMap<String, Object>());
					if (!map.containsKey("index")) map.put("index", "index.class,index.jwsl,index.php,index.html");
					if (!map.containsKey("cacheClock")) map.put("cacheClock", "-1");
					if (!map.containsKey("dns")) map.put("dns", "false");
					if (!map.containsKey("dnsf")) map.put("dnsf", new File(dir, "dns.cfg").toString());
					if (!map.containsKey("com")) map.put("com", new HashMap<String, Object>());
					HashMap<String, Object> telnet = (HashMap<String, Object>)map.get("com");
					if (!telnet.containsKey("enabled")) telnet.put("enabled", "true");
					if (!telnet.containsKey("bindport")) telnet.put("bindport", "6049");
					if (!telnet.containsKey("bindip")) telnet.put("bindip", "127.0.0.1");
					if (!telnet.containsKey("auth")) telnet.put("auth", "jwsisawesome");
					if (!telnet.containsKey("doAuth")) telnet.put("doAuth", "true");
				}
			});
			mainConfig.load();
			mainConfig.save();
			final int cl = Integer.parseInt((String)mainConfig.get("connlimit", null));
			hostsConfig = new Config("hosts", new File((String)mainConfig.get("hosts", null)), new ConfigFormat() {
				
				@Override
				public void format(HashMap<String, Object> map) {
					if (!map.containsKey("main")) map.put("main", new HashMap<String, Object>());
					for (String key : map.keySet()) {
						HashMap<String, Object> host = (HashMap<String, Object>)map.get(key);
						if (!host.containsKey("port")) host.put("port", "80");
						if (!host.containsKey("ip")) host.put("ip", "0.0.0.0");
						if (!host.containsKey("vhosts")) host.put("vhosts", new HashMap<String, Object>());
						HashMap<String, Object> vhosts = (HashMap<String, Object>)host.get("vhosts");
						if (!vhosts.containsKey("main")) vhosts.put("main", new HashMap<String, Object>());
						for (String vkey : vhosts.keySet()) {
							HashMap<String, Object> vhost = (HashMap<String, Object>)vhosts.get(vkey);
							if (!vhost.containsKey("enabled")) vhost.put("enabled", "true");
							if (!vhost.containsKey("debug")) vhost.put("debug", "false");
							if (!vhost.containsKey("host")) vhost.put("host", ".*");
							if (!vhost.containsKey("htdocs")) vhost.put("htdocs", fileManager.getBaseFile("htdocs").toString());
							if (!vhost.containsKey("htsrc")) vhost.put("htsrc", fileManager.getBaseFile("htsrc").toString());
						}
						if (!host.containsKey("ssl")) host.put("ssl", new HashMap<String, Object>());
						HashMap<String, Object> ssl = (HashMap<String, Object>)host.get("ssl");
						if (!ssl.containsKey("enabled")) ssl.put("enabled", "false");
						if (!ssl.containsKey("keyFile")) ssl.put("keyFile", fileManager.getBaseFile("ssl/keyFile").toString());
						if (!ssl.containsKey("keystorePassword")) ssl.put("keystorePassword", "password");
						if (!ssl.containsKey("keyPassword")) ssl.put("keyPassword", "password");
						if (!host.containsKey("masterOverride")) host.put("masterOverride", new HashMap<String, Object>());
						HashMap<String, Object> masterOverride = (HashMap<String, Object>)host.get("masterOverride");
						Host h = new Host(key, (String)host.get("ip"), Integer.parseInt((String)host.get("port")), cl, masterOverride, ssl.get("enabled").equals("true"), new File((String)ssl.get("keyFile")), (String)ssl.get("keyPassword"), (String)ssl.get("keystorePassword"));
						for (String vkey : vhosts.keySet()) {
							HashMap<String, Object> ourvh = (HashMap<String, Object>)vhosts.get(vkey);
							if (!ourvh.get("enabled").equals("true")) continue;
							VHost vhost = new VHost(h.getHostname() + "/" + vkey, h, new File((String)ourvh.get("htdocs")), new File((String)ourvh.get("htsrc")), (String)ourvh.get("host"));
							vhost.setDebug(ourvh.get("debug").equals("true"));
							h.addVHost(vhost);
						}
						hosts.put(key, h);
					}
				}
				
			});
			hostsConfig.load();
			hostsConfig.save();
			boolean dns = mainConfig.get("dns", null).equals("true");
			RecordHolder holder = null;
			if (dns) {
				holder = new RecordHolder(new File((String)mainConfig.get("dnsf", null)));
			}
			setupFolders();
			File lf = new File(fileManager.getLogs(), "" + (System.currentTimeMillis() / 1000L));
			lf.createNewFile();
			Logger.INSTANCE = new Logger(new PrintStream(new FileOutputStream(lf)));
			unpack();
			loadUnpacked();
			Logger.log("Loaded Configs");
			Logger.log("Loading Connection Handling");
			ThreadWorker.initQueue(cl < 1 ? 10000000 : cl);
			for (int i = 0; i < Integer.parseInt((String)JavaWebServer.mainConfig.get("workerThreadCount", null)); i++) {
				ThreadWorker worker = new ThreadWorker();
				worker.start();
			}
			if (dns) { // TODO maybe split off into different cfgs than above
				ThreadDNSWorker.holder = holder;
				ThreadDNSWorker.initQueue(cl < 1 ? 10000000 : cl);
				for (int i = 0; i < Integer.parseInt((String)JavaWebServer.mainConfig.get("workerThreadCount", null)); i++) {
					ThreadDNSWorker worker = new ThreadDNSWorker();
					worker.start();
				}
			}
			Logger.log("Loading Plugins");
			BaseLoader.loadBases();
			HashMap<String, Object> com = ((HashMap<String, Object>)mainConfig.get("com", null));
			if (((String)com.get("enabled")).equals("true")) {
				Logger.log("Starting Com Server on " + ((String)com.get("bindip")) + ":" + ((String)com.get("bindport")));
				ComServer server = new ComServer();
				server.start();
			}
			if (dns) {
				Logger.log("Starting DNS Nameserver on 0.0.0.0:53 (UDP+TCP)");
				UDPServer udp = new UDPServer();
				udp.start();
				TCPServer tcp = new TCPServer();
				tcp.start();
			}
			for (Host host : hosts.values()) {
				host.start();
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
