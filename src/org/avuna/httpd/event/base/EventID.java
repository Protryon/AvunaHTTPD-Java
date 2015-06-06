package org.avuna.httpd.event.base;

public abstract class EventID {
	public static final int CONNECTED = 0;
	public static final int DISCONNECTED = 1;
	public static final int POSTINIT = 2;
	public static final int PREEXIT = 3;
	public static final int RELOAD = 4;
	public static final int SETUPFOLDERS = 5;
	public static final int PRECONNECT = 6;
}
