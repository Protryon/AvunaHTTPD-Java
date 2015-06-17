/*	Avuna HTTPD - General Server Applications
    Copyright (C) 2015 Maxwell Bruce

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package org.avuna.httpd.util.unixsocket;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import org.avuna.httpd.util.CLib;

public class UnixServerSocket extends ServerSocket {
	private int sockfd = 0;
	private boolean bound = false;
	private String file = "";
	private int backlog = 50;
	private boolean closed = false;
	
	public boolean isClosed() {
		return closed;
	}
	
	public UnixServerSocket(String file, int backlog) throws IOException {
		this.backlog = backlog;
		File f = new File(file);
		f.getParentFile().mkdirs();
		f.delete();
		this.file = f.getAbsolutePath();
	}
	
	public UnixServerSocket(String file) throws IOException {
		this(file, 50);
	}
	
	public void bind() throws IOException {
		if (bound) throw new IOException("Already bound!");
		sockfd = CLib.socket(1, 1, 0);
		if (sockfd < 0) throw new CException(CLib.errno(), "socket failed native create");
		int bind = CLib.bind(sockfd, file);
		if (bind != 0) throw new CException(CLib.errno(), "socket failed bind");
		int listen = CLib.listen(sockfd, this.backlog);
		if (listen != 0) throw new CException(CLib.errno(), "socket failed listen");
		bound = true;
	}
	
	public UnixSocket accept() throws IOException {
		if (!bound) bind();
		// Logger.log("accepting");
		String nsfd = CLib.accept(sockfd);
		// Logger.log(nsfd);
		int i = Integer.parseInt(nsfd.substring(0, nsfd.indexOf("/")));
		nsfd = nsfd.substring(nsfd.indexOf("/") + 1);
		UnixSocket us = new UnixSocket(file, i);
		return us;
	}
	
	public void close() throws IOException {
		closed = true;
		int s = CLib.close(sockfd);
		if (s < 0) throw new CException(CLib.errno(), "socket failed close");
	}
}
