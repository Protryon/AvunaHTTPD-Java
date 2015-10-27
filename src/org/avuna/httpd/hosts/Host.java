/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.hosts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.event.Event;
import org.avuna.httpd.event.EventBus;
import org.avuna.httpd.event.IEventReceiver;
import org.avuna.httpd.event.base.EventID;
import org.avuna.httpd.event.base.EventReload;
import org.avuna.httpd.util.CLib;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.logging.Logger;
import org.avuna.httpd.util.unio.Certificate;
import org.avuna.httpd.util.unio.PacketReceiver;
import org.avuna.httpd.util.unio.PacketReceiverFactory;
import org.avuna.httpd.util.unio.SNICallback;
import org.avuna.httpd.util.unio.UNIOServerSocket;
import org.avuna.httpd.util.unixsocket.UnixServerSocket;

public abstract class Host extends Thread implements ITerminatable, IEventReceiver {
	protected final String name;
	protected final Protocol protocol;
	private boolean isStarted = false;
	public final EventBus eventBus;
	public final Logger logger;
	
	public Host(String threadName, Protocol protocol) {
		super(threadName + " Host");
		this.name = threadName;
		this.logger = new Logger(this);
		this.protocol = protocol;
		eventBus = new EventBus();
		eventBus.registerEvent(EventID.RELOAD, this, 0);
	}
	
	public void receive(EventBus bus, Event event) {
		if (event instanceof EventReload) {
			formatConfig(getConfig());
		}
	}
	
	public void setupFolders() {
	
	}
	
	private ConfigNode virtualConfig = null;
	
	public boolean hasVirtualConfig() {
		return virtualConfig != null;
	}
	
	public void setVirtualConfig(ConfigNode node) {
		this.virtualConfig = node;
	}
	
	public final ConfigNode getConfig() {
		return virtualConfig != null ? virtualConfig : AvunaHTTPD.hostsConfig.getNode(name);
	}
	
	public boolean loaded = false;
	private List<ITerminatable> terms = Collections.synchronizedList(new ArrayList<ITerminatable>());
	
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
				logger.logError(e);
			}
		}
	}
	
	public final ServerSocket makeServer(String ip, int port) throws IOException {
		return makeServer(ip, port, ssl);
	}
	
	public SNICallback makeSNICallback() {
		return null;
	}
	
	public final ServerSocket makeServer(String ip, int port, boolean ssl) throws IOException {
		logger.log("Starting " + name + "/" + protocol.name + " " + (ssl ? "TLS-" : "") + "Server on " + ip + ":" + port);
		if (ssl) {
			try {
				if (nssl) {
					UNIOServerSocket server = new UNIOServerSocket(ip, port, new PacketReceiverFactory() {
						
						@Override
						public PacketReceiver newCallback(UNIOServerSocket server) {
							return makeReceiver(server);
						}
						
					}, 1000, new Certificate(caFile, certFile, pkFile), makeSNICallback());
					servers.add(server);
					return server;
				}else {
					ServerSocket server = sslContext.getServerSocketFactory().createServerSocket(port, 1000, InetAddress.getByName(ip));
					servers.add(server);
					return server;
				}
			}catch (Exception e) {
				logger.logError(e);
				return null;
			}
		}else {
			ServerSocket server;
			if (unio()) {
				server = new UNIOServerSocket(ip, port, new PacketReceiverFactory() {
					
					@Override
					public PacketReceiver newCallback(UNIOServerSocket server) {
						return makeReceiver(server);
					}
					
				}, 1000);
			}else {
				server = new ServerSocket();
				server.setReuseAddress(true);
				server.bind(new InetSocketAddress(InetAddress.getByName(ip), port), 1000);
			}
			if (server instanceof UNIOServerSocket) ((UNIOServerSocket) server).bind();
			servers.add(server);
			return server;
		}
	}
	
	public final UnixServerSocket makeUnixServer(String file) throws IOException {
		logger.log("Starting " + name + "/" + protocol.name + " " + "Server on " + file);
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
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] certs, String authType) {}
				
				public void checkServerTrusted(X509Certificate[] certs, String authType) {}
				
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			} };
			SSLContext sc = null;
			String[] possibleProtocols = new String[] { "TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1", "TLSv1.0" };
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
				logger.log(name + ": No suitable TLS protocols found, please upgrade Java! Host not loaded.");
				return null;
			}
			sc.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
			return sc;
		}catch (Exception e) {
			logger.logError(e);
			return null;
		}
	}
	
	public SSLContext sslContext = null;
	protected long cert = 0L;
	protected String certFile, pkFile, caFile;
	protected boolean nssl = false;
	protected boolean ssl = false;
	
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
			if (cfg.containsNode("unix") && cfg.getNode("unix").getValue().equals("true")) {
				iu = true;
				setup(makeUnixServer(cfg.getNode("ip").getValue()));
			}else {
				iu = false;
				setup(makeServer(cfg.getNode("ip").getValue(), Integer.parseInt(cfg.getNode("port").getValue())));
			}
		}catch (Exception e) {
			logger.logError(e);
			logger.log("Closing " + name + "/" + protocol.name + " Server on " + getConfig().getNode("ip").getValue() + ":" + getConfig().getNode("port").getValue());
			// TODO: destroy
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
		if (CLib.failed || CLib.hasGNUTLS() == 0) {
			if (!ssl.containsNode("keyFile")) ssl.insertNode("keyFile", AvunaHTTPD.fileManager.getBaseFile("ssl/keyFile").toString());
			if (!ssl.containsNode("keystorePassword")) ssl.insertNode("keystorePassword", "password");
			if (!ssl.containsNode("keyPassword")) ssl.insertNode("keyPassword", "password");
		}else {
			if (!ssl.containsNode("cert")) ssl.insertNode("cert", AvunaHTTPD.fileManager.getBaseFile("ssl/ssl.cert").getAbsolutePath());
			if (!ssl.containsNode("privateKey")) ssl.insertNode("privateKey", AvunaHTTPD.fileManager.getBaseFile("ssl/ssl.pem").getAbsolutePath());
			if (!ssl.containsNode("ca")) ssl.insertNode("ca", AvunaHTTPD.fileManager.getBaseFile("ssl/ca.cert").getAbsolutePath());
		}
	}
	
	public String getHostname() {
		return name;
	}
	
	public int getPort() {
		return Integer.parseInt((String) getConfig().getNode("port").getValue());
	}
	
	public abstract void setup(ServerSocket s);
	
	public void preExit() {
	
	}
	
	public boolean unio() {
		ConfigNode node = getConfig();
		return enableUNIO() && !CLib.failed && (!node.containsNode("unix") || !node.getValue("unix").equals("true"));
	}
	
	public PacketReceiver makeReceiver(UNIOServerSocket server) {
		return null;
	}
	
	public boolean enableUNIO() {
		return false;
	}
}
