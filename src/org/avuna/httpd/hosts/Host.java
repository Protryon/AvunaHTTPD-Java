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
import java.util.HashMap;
import java.util.LinkedHashMap;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.util.Logger;

public abstract class Host extends Thread {
	protected final String name;
	protected final Protocol protocol;
	
	public Host(String threadName, Protocol protocol) {
		super(threadName + " Host");
		this.name = threadName;
		this.protocol = protocol;
	}
	
	public void setupFolders() {
		
	}
	
	public final LinkedHashMap<String, Object> getConfig() {
		return (LinkedHashMap<String, Object>)AvunaHTTPD.hostsConfig.get(name);
	}
	
	public final ServerSocket makeServer(String ip, int port, boolean ssl, File keyFile, String keyPassword, String keystorePassword) throws IOException {
		Logger.log("Starting " + name + "/" + protocol.name + " " + (ssl ? "TLS-" : "") + "Server on " + ip + ":" + port);
		if (ssl) {
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
					public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
					}
					
					public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
					}
					
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}
				}};
				SSLContext sc = null;
				String[] possibleProtocols = new String[]{"TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1", "TLSv1.0"};
				String fp = "";
				for (String protocol : possibleProtocols) {
					try {
						sc = SSLContext.getInstance(protocol);
						fp = protocol;
					}catch (NoSuchAlgorithmException e) {
						continue;
					}
				}
				if (sc == null) {
					Logger.log(name + ": No suitable TLS protocols found, please upgrade Java! Host not loaded.");
					return null;
				}
				sc.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
				ServerSocket server = (SSLServerSocket)sc.getServerSocketFactory().createServerSocket(port, 1000, InetAddress.getByName(ip));
				((SSLServerSocket)server).setEnabledProtocols(new String[]{fp});
				return server;
			}catch (Exception e) {
				Logger.logError(e);
				return null;
			}
		}else {
			return new ServerSocket(port, 1000, InetAddress.getByName(ip));
		}
	}
	
	public void run() {
		try {
			LinkedHashMap<String, Object> cfg = getConfig();
			LinkedHashMap<String, Object> ssl = (LinkedHashMap<String, Object>)cfg.get("ssl");
			boolean isSSL = !(ssl == null || !ssl.get("enabled").equals("true"));
			setup(makeServer((String)cfg.get("ip"), Integer.parseInt((String)cfg.get("port")), isSSL, isSSL ? new File((String)ssl.get("keyFile")) : null, isSSL ? (String)ssl.get("keyPassword") : "", isSSL ? (String)ssl.get("keystorePassword") : ""));
		}catch (Exception e) {
			Logger.logError(e);
			Logger.log("Closing " + name + "/" + protocol.name + " Server on " + (String)getConfig().get("ip") + ":" + (String)getConfig().get("port"));
		}finally {
			
		}
	}
	
	public void formatConfig(HashMap<String, Object> map) {
		if (!map.containsKey("port")) map.put("port", "80");
		if (!map.containsKey("ip")) map.put("ip", "0.0.0.0");
		if (!map.containsKey("ssl")) map.put("ssl", new LinkedHashMap<String, Object>());
		HashMap<String, Object> ssl = (HashMap<String, Object>)map.get("ssl");
		if (!ssl.containsKey("enabled")) ssl.put("enabled", "false");
		if (!ssl.containsKey("keyFile")) ssl.put("keyFile", AvunaHTTPD.fileManager.getBaseFile("ssl/keyFile").toString());
		if (!ssl.containsKey("keystorePassword")) ssl.put("keystorePassword", "password");
		if (!ssl.containsKey("keyPassword")) ssl.put("keyPassword", "password");
	}
	
	public String getHostname() {
		return name;
	}
	
	public int getPort() {
		return Integer.parseInt((String)getConfig().get("port"));
	}
	
	public abstract void setup(ServerSocket s);
}
