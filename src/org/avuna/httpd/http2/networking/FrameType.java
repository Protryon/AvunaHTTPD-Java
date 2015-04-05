package org.avuna.httpd.http2.networking;

public enum FrameType {
	DATA(0, false, true, true, true, 9), //
	HEADERS(1, false, true, true, false, 45), //
	PRIORITY(2, false, false, true, false, 0), //
	RST_STREAM(3, false, false, true, false, 0), //
	SETTINGS(4, false, true, false, false, 1), //
	PUSH_PROMISE(5, false, false, true, false, 12), //
	PING(6, false, false, false, false, 1), //
	GOAWAY(7, false, false, false, false, 0), //
	WINDOW_UPDATE(8, false, true, false, false, 0), //
	CONTINUATION(9, false, true, true, true, 4);
	public final int id;
	/**
	 * require(read/write) is in the perspective of writing to the clients, and is flipped for reading(if ever read).
	 */
	public final boolean requireRead, requireWrite, requireStream, requireOpen;
	public final int flagmask;
	
	private FrameType(int id, boolean requireRead, boolean requireWrite, boolean requireStream, boolean requireOpen, int flagmask) {
		this.id = id;
		this.requireRead = requireRead;
		this.requireWrite = requireWrite;
		this.requireStream = requireStream;
		this.requireOpen = requireOpen;
		this.flagmask = flagmask;
	}
	
	public static FrameType getByID(int id) {
		for (FrameType type : values()) {
			if (type.id == id) return type;
		}
		return null;
	}
}
