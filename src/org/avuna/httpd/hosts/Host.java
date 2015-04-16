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
import java.util.HashMap;
import java.util.LinkedHashMap;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
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
	
	public boolean loaded = false;
	
	public final ServerSocket makeServer(String ip, int port, boolean ssl, SSLServerSocketFactory sc) throws IOException {
		Logger.log("Starting " + name + "/" + protocol.name + " " + (ssl ? "TLS-" : "") + "Server on " + ip + ":" + port);
		if (ssl) {
			try {
				ServerSocket server = sc.createServerSocket(port, 1000, InetAddress.getByName(ip));
				// ((SSLServerSocket)server).setEnabledProtocols(possibleProtocols);
				return server;
			}catch (Exception e) {
				Logger.logError(e);
				return null;
			}
		}else {
			return new ServerSocket(port, 1000, InetAddress.getByName(ip));
		}
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
	
	public void run() {
		try {
			LinkedHashMap<String, Object> cfg = getConfig();
			LinkedHashMap<String, Object> ssl = (LinkedHashMap<String, Object>)cfg.get("ssl");
			boolean isSSL = !(ssl == null || !ssl.get("enabled").equals("true"));
			if (isSSL) {
				sslContext = makeSSLContext(new File((String)ssl.get("keyFile")), (String)ssl.get("keyPassword"), (String)ssl.get("keystorePassword"));
			}
			setup(makeServer((String)cfg.get("ip"), Integer.parseInt((String)cfg.get("port")), isSSL, !isSSL ? null : sslContext.getServerSocketFactory()));
		}catch (Exception e) {
			Logger.logError(e);
			Logger.log("Closing " + name + "/" + protocol.name + " Server on " + (String)getConfig().get("ip") + ":" + (String)getConfig().get("port"));
		}finally {
			loaded = true;
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
	
	public void preExit() {
		
	}
}
