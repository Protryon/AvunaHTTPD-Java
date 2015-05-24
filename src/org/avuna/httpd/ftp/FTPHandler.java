package org.avuna.httpd.ftp;

import java.io.IOException;
import java.util.ArrayList;
import org.avuna.httpd.hosts.HostFTP;

public class FTPHandler {
	
	public final ArrayList<FTPCommand> commands = new ArrayList<FTPCommand>();
	
	public FTPHandler(final HostFTP host) {
		
		commands.add(new FTPCommand("quit", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				focus.writeLine(221, host.getConfig().getNode("domain").getValue().split(",")[0] + " terminating connection.");
				focus.s.close();
			}
		});
	}
}
