package org.avuna.httpd.hosts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.Logger;
import org.avuna.httpd.util.unixsocket.UnixServerSocket;

public abstract class Host extends Thread implements ITerminatable {
	protected final String name;
	protected final Protocol protocol;
	private boolean isStarted = false;
	
	public Host(String threadName, Protocol protocol) {
		super(threadName + " Host");
		this.name = threadName;
		this.protocol = protocol;
	}
	
	public void setupFolders() {
		
	}
	
	private ConfigNode virtualConfig = null;
	
	public void setVirtualConfig(ConfigNode node) {
		this.virtualConfig = node;
	}
	
	public final ConfigNode getConfig() {
		return virtualConfig != null ? virtualConfig : AvunaHTTPD.hostsConfig.getNode(name);
	}
	
	public boolean loaded = false;
	private ArrayList<ITerminatable> terms = new ArrayList<ITerminatable>();
	
	public void addTerm(ITerminatable it) {
		terms.add(it);
	}
	
	private ArrayList<ServerSocket> servers = new ArrayList<ServerSocket>();
	
	public void postload() throws IOException {
		
	}
	
	@Override
	public void terminate() {
		for (ITerminatable worker : terms) {
			worker.terminate();
		}
		for (ServerSocket server : servers) {
			try {
				server.close();
			}catch (IOException e) {
				Logger.logError(e);
			}
		}
	}
	
	public final ServerSocket makeServer(String ip, int port, boolean ssl, SSLServerSocketFactory sc) throws IOException {
		Logger.log("Starting " + name + "/" + protocol.name + " " + (ssl ? "TLS-" : "") + "Server on " + ip + ":" + port);
		if (ssl) {
			try {
				ServerSocket server = sc.createServerSocket(port, 50, InetAddress.getByName(ip));
				servers.add(server);
				return server;
			}catch (Exception e) {
				Logger.logError(e);
				return null;
			}
		}else {
			ServerSocket server = new ServerSocket(port, 1000, InetAddress.getByName(ip));
			servers.add(server);
			return server;
		}
	}
	
	public final UnixServerSocket makeUnixServer(String file) throws IOException {
		Logger.log("Starting " + name + "/" + protocol.name + " " + "Server on " + file);
		UnixServerSocket uss = new UnixServerSocket(file);
		servers.add(uss);
		return uss;
	}
	
	public final SSLContext makeSSLContext(File keyFile, String keyPassword, String keystorePassword) throws IOException {
		try {
			KeyStore ks = KeyStore.getInstance("JKS");
			InputStream ksIs = new FileInputStream(keyFile);
			try {
				ks.load(ksIs, keystorePassword.toCharArray());
			}finally {
				if (ksIs != null) {
					ksIs.close();
				}
			}
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(ks, keyPassword.toCharArray());
			TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}
				
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
				
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			}};
			SSLContext sc = null;
			String[] possibleProtocols = new String[]{"TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1", "TLSv1.0"};
			for (int i = 0; i < possibleProtocols.length; i++) {
				try {
					sc = SSLContext.getInstance(possibleProtocols[i]);
					String[] tp = new String[possibleProtocols.length - i];
					System.arraycopy(possibleProtocols, i, tp, 0, tp.length);
					possibleProtocols = tp;
					break;
				}catch (NoSuchAlgorithmException e) {
					continue;
				}
			}
			if (sc == null) {
				Logger.log(name + ": No suitable TLS protocols found, please upgrade Java! Host not loaded.");
				return null;
			}
			sc.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
			return sc;
		}catch (Exception e) {
			Logger.logError(e);
			return null;
		}
	}
	
	public SSLContext sslContext = null;
	
	public boolean hasStarted() {
		return isStarted;
	}
	
	private boolean iu = false;
	
	public boolean isUnix() {
		return iu;
	}
	
	public void run() {
		isStarted = true;
		try {
			ConfigNode cfg = getConfig();
			ConfigNode ssl = cfg.getNode("ssl");
			boolean isSSL = !(ssl == null || !ssl.getNode("enabled").getValue().equals("true"));
			if (isSSL) {
				sslContext = makeSSLContext(new File(ssl.getNode("keyFile").getValue()), ssl.getNode("keyPassword").getValue(), ssl.getNode("keystorePassword").getValue());
			}
			if (cfg.containsNode("unix") && cfg.getNode("unix").getValue().equals("true")) {
				iu = true;
				setup(makeUnixServer(cfg.getNode("ip").getValue()));
			}else {
				iu = false;
				setup(makeServer(cfg.getNode("ip").getValue(), Integer.parseInt(cfg.getNode("port").getValue()), isSSL, !isSSL ? null : sslContext.getServerSocketFactory()));
			}
		}catch (Exception e) {
			Logger.logError(e);
			Logger.log("Closing " + name + "/" + protocol.name + " Server on " + getConfig().getNode("ip").getValue() + ":" + getConfig().getNode("port").getValue());
		}finally {
			loaded = true;
		}
	}
	
	public void formatConfig(ConfigNode map) {
		if (!map.containsNode("port")) map.insertNode("port", "80");
		if (!map.containsNode("unix")) map.insertNode("unix", "false", "set to true, and set ip to the socket file to use a unix socket. port is ignored. mostly used for shared hosting.");
		if (!map.containsNode("ip")) map.insertNode("ip", "0.0.0.0");
		if (!map.containsNode("ssl")) map.insertNode("ssl");
		ConfigNode ssl = map.getNode("ssl");
		if (!ssl.containsNode("enabled")) ssl.insertNode("enabled", "false");
		if (!ssl.containsNode("keyFile")) ssl.insertNode("keyFile", AvunaHTTPD.fileManager.getBaseFile("ssl/keyFile").toString());
		if (!ssl.containsNode("keystorePassword")) ssl.insertNode("keystorePassword", "password");
		if (!ssl.containsNode("keyPassword")) ssl.insertNode("keyPassword", "password");
	}
	
	public String getHostname() {
		return name;
	}
	
	public int getPort() {
		return Integer.parseInt((String)getConfig().getNode("port").getValue());
	}
	
	public abstract void setup(ServerSocket s);
	
	public void preExit() {
		
	}
}
