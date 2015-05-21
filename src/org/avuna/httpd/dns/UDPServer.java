package org.avuna.httpd.dns;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import org.avuna.httpd.util.Logger;

/**
 * Created by JavaProphet on 8/13/14 at 10:56 PM.
 */
public class UDPServer extends Thread implements IServer {
	
	public UDPServer() {
		super("DNS UDPServer");
	}
	
	public boolean bound = false;
	
	public void run() {
		DatagramSocket server = null;
		try {
			server = new DatagramSocket(53);
			bound = true;
			while (!server.isClosed()) {
				byte[] rec = new byte[1024];
				final DatagramPacket receive = new DatagramPacket(rec, rec.length);
				server.receive(receive);
				ThreadDNSWorker.addWork(new WorkUDP(receive.getData(), receive.getAddress(), receive.getPort(), server));
			}
		}catch (Exception e) {
			Logger.logError(e);
		}finally {
			if (server != null) server.close();
			bound = true;
		}
	}
}
