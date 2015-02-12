package com.javaprophet.javawebserver.dns;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by JavaProphet on 8/13/14 at 10:56 PM.
 */
public class UDPServer extends Thread implements IServer {
	
	public UDPServer() {
		
	}
	
	public void run() {
		DatagramSocket server = null;
		try {
			server = new DatagramSocket(53);
			while (!server.isClosed()) {
				byte[] rec = new byte[1024];
				final DatagramPacket receive = new DatagramPacket(rec, rec.length);
				server.receive(receive);
				ThreadDNSWorker.addWork(new WorkUDP(receive.getData(), receive.getAddress(), receive.getPort(), server));
			}
		}catch (Exception e) {
			e.printStackTrace();
		}finally {
			if (server != null) server.close();
		}
	}
}
