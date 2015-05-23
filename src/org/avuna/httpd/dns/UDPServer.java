package org.avuna.httpd.dns;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import org.avuna.httpd.hosts.HostDNS;
import org.avuna.httpd.hosts.ITerminatable;
import org.avuna.httpd.util.Logger;

/**
 * Created by JavaProphet on 8/13/14 at 10:56 PM.
 */
public class UDPServer extends Thread implements IServer, ITerminatable {
	private HostDNS host;
	
	public UDPServer(HostDNS host) {
		super("DNS UDPServer");
		this.host = host;
	}
	
	public boolean bound = false;
	private DatagramSocket server = null;
	
	public void run() {
		try {
			server = new DatagramSocket(Integer.parseInt(host.getConfig().getNode("port").getValue()));
			bound = true;
			while (!server.isClosed()) {
				try {
					byte[] rec = new byte[1024];
					final DatagramPacket receive = new DatagramPacket(rec, rec.length);
					server.receive(receive);
					host.addWork(new WorkUDP(receive.getData(), receive.getAddress(), receive.getPort(), server));
				}catch (SocketException e2) {
				}catch (Exception e) {
					Logger.logError(e);
				}
			}
		}catch (Exception e) {
			Logger.logError(e);
		}finally {
			if (server != null) server.close();
			bound = true;
		}
	}
	
	@Override
	public void terminate() {
		if (server != null) {
			server.close();
		}
		this.interrupt();
	}
}
