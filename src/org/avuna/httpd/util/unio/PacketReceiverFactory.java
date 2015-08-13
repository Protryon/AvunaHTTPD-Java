package org.avuna.httpd.util.unio;

public abstract class PacketReceiverFactory {
	public abstract PacketReceiver newCallback(UNIOServerSocket server);
}
