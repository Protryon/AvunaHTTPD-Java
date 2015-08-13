/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.imap;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.mailbox.EmailAccount;
import org.avuna.httpd.mail.mailbox.Mailbox;
import org.avuna.httpd.mail.util.StringFormatter;
import org.avuna.httpd.util.unio.UNIOSocket;

public class IMAPWork {
	public Socket s;
	public DataInputStream in;
	public DataOutputStream out;
	public boolean ssl;
	public int state = 0;
	public String authMethod = "";
	public EmailAccount authUser = null;
	public Mailbox selectedMailbox = null;
	public boolean isExamine = false;
	public long sns = 0L;
	public int tos = 0;
	public ByteArrayOutputStream sslprep = null;
	public HostMail host;
	public boolean inUse = false;
	
	public void readLine(String rline) throws IOException {
		String line = rline.trim();
		host.logger.log(hashCode() + ": " + line);
		String cmd;
		String letters;
		String[] args;
		if (!(state == 1)) {
			if (!line.contains(" ") || line.length() == 0) return;
			letters = line.substring(0, line.indexOf(" "));
			line = line.substring(letters.length() + 1);
			cmd = line.substring(0, line.contains(" ") ? line.indexOf(" ") : line.length()).toLowerCase();
			line = line.substring(cmd.length()).trim();
			args = StringFormatter.congealBySurroundings(line.split(" "), "(", ")");
		}else {
			letters = line;
			cmd = "";
			args = new String[0];
		}
		boolean r = false;
		for (IMAPCommand comm : host.imaphandler.commands) {
			if ((state == 1 ? comm.comm.equals("") : comm.comm.equals(cmd)) && comm.minState <= state && comm.maxState >= state) {
				comm.run(this, letters, args);
				r = true;
				break;
			}
		}
		if (!r) {
			writeLine(letters, "BAD Command not recognized");
		}
	}
	
	public void close() throws IOException {
		s.close();
		host.IMAPworks.remove(this);
	}
	
	public void flushPacket(byte[] buf) throws IOException {
		readLine(new String(buf));
	}
	
	public IMAPWork(HostMail host, Socket s, DataInputStream in, DataOutputStream out, boolean ssl) {
		this.host = host;
		this.s = s;
		this.in = in;
		this.out = out;
		this.ssl = ssl;
		if (ssl) {
			sslprep = new ByteArrayOutputStream();
		}
		if (host.unio()) {
			((IMAPPacketReceiver) ((UNIOSocket) s).getCallback()).setWork(this);
		}
	}
	
	public void writeLine(String id, String data) throws IOException {
		host.logger.log(hashCode() + ": " + id + " " + data);
		out.write(((id.length() > 0 ? (id + " ") : "") + data + AvunaHTTPD.crlf).getBytes());
	}
}
