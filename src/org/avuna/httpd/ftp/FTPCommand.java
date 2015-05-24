package org.avuna.httpd.ftp;

import java.io.IOException;

public abstract class FTPCommand {
	public final String comm;
	public final int minState, maxState;
	
	public FTPCommand(String comm, int minState, int maxState) {
		this.comm = comm;
		this.minState = minState;
		this.maxState = maxState;
	}
	
	public abstract void run(FTPWork focus, String line) throws IOException;
}
