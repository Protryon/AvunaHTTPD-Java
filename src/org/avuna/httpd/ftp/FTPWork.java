/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.ftp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostFTP;
import org.avuna.httpd.util.unio.UNIOSocket;

public class FTPWork {
	public Socket s;
	public DataInputStream in;
	public DataOutputStream out;
	public int state = 0;
	public long sns = 0L;
	public int tos = 0;
	public String user = "";
	public boolean auth = false;
	public String cwd = "/";
	public File root = new File("/");
	public FTPType type = FTPType.ASCII;
	public boolean isPASV = false, isPORT = false;
	public ThreadPassive psv = null;
	public int port = 0;
	public File rnfr = null;
	public int skip = 0;
	public HostFTP host;
	public boolean inUse = false;
	public ArrayBlockingQueue<String> outQueue;
	
	public FTPWork(Socket s, DataInputStream in, DataOutputStream out, HostFTP host) {
		this.s = s;
		this.in = in;
		this.out = out;
		this.host = host;
		if (host.unio()) {
			((FTPPacketReceiver) ((UNIOSocket) s).getCallback()).setWork(this);
		}
	}
	
	public void close() throws IOException {
		s.close();
		if (psv != null) {
			psv.cancel();
		}
		host.works.remove(this);
	}
	
	public void writeBMLine(int response, String data) throws IOException {
		out.write((response + "-" + data + AvunaHTTPD.crlf).getBytes());
	}
	
	public void writeMMLine(String data) throws IOException {
		out.write((" " + data + AvunaHTTPD.crlf).getBytes());
	}
	
	public void writeLine(int response, String data) throws IOException {
		out.write((response + " " + data + AvunaHTTPD.crlf).getBytes());
	}
	
	public void readLine(String rline) throws IOException {
		String line = rline.trim();
		String cmd = "";
		if (state != 101) {
			cmd = line.contains(" ") ? line.substring(0, line.indexOf(" ")) : line;
			cmd = cmd.toLowerCase();
			line = line.substring(cmd.length()).trim();
		}
		boolean r = false;
		for (FTPCommand comm : host.ftphandler.commands) {
			if (comm.comm.equals(cmd)) {
				if (state <= comm.maxState && state >= comm.minState) {
					comm.run(this, line);
				}else {
					writeLine(500, "Command not allowed at this time.");
				}
				r = true;
				break;
			}
		}
		if (!r) {
			writeLine(500, "Command not recognized");
		}
	}
	
	public void flushPacket(byte[] buf) throws IOException {
		readLine(new String(buf));
	}
}
