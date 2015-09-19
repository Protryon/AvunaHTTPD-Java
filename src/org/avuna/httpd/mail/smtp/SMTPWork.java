/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.smtp;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.mailbox.EmailAccount;
import org.avuna.httpd.util.unio.UNIOSocket;

public class SMTPWork {
	public Socket s;
	public DataInputStream in;
	public DataOutputStream out;
	public boolean ssl;
	public int state = 0;
	public boolean isExtended = false;
	public EmailAccount authUser = null;
	public String mailFrom = "";
	public long sns = 0L;
	public int tos = 0;
	public ArrayList<String> rcptTo = new ArrayList<String>();
	public ArrayList<String> data = new ArrayList<String>();
	public ByteArrayOutputStream sslprep = null;
	public HostMail host;
	public boolean inUse = false;
	public String lu = null;
	
	public SMTPWork(HostMail host, Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		this.host = host;
		this.s = s;
		this.in = in;
		this.out = out;
		this.ssl = ssl;
		if (ssl) {
			sslprep = new ByteArrayOutputStream();
		}
		if (host.unio()) {
			((SMTPPacketReceiver) ((UNIOSocket) s).getCallback()).setWork(this);
		}
	}
	
	public void close() throws IOException {
		s.close();
		host.SMTPworks.remove(this);
	}
	
	public void flushPacket(byte[] buf) throws IOException {
		readLine(new String(buf, 0, buf.length - 2)); // remove ending crlf
	}
	
	public void readLine(String rline) throws IOException {
		String line = rline;
		host.logger.log(hashCode() + ": " + line);
		String cmd = "";
		if (state < 101) {
			line = line.trim();
			cmd = line.contains(" ") ? line.substring(0, line.indexOf(" ")) : line;
			cmd = cmd.toLowerCase().trim();
			line = line.substring(cmd.length()).trim();
		}
		boolean r = false;
		for (SMTPCommand comm : host.smtphandler.commands) {
			if ((state > 100 || comm.comm.equals(cmd)) && state <= comm.maxState && state >= comm.minState) {
				comm.run(this, line);
				r = true;
				break;
			}
		}
		if (!r) {
			writeLine(500, "Command not recognized");
		}
	}
	
	public void writeMLine(int response, String data) throws IOException {
		host.logger.log(hashCode() + ": " + response + "-" + data);
		out.write((response + "-" + data + AvunaHTTPD.crlf).getBytes());
	}
	
	public void writeLine(int response, String data) throws IOException {
		host.logger.log(hashCode() + ": " + response + " " + data);
		out.write((response + " " + data + AvunaHTTPD.crlf).getBytes());
	}
}
