/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.ftp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Scanner;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostFTP;
import org.avuna.httpd.util.unio.UNIOSocket;

public class ThreadPassive extends Thread {
	private Object serv = null;
	private int ep = -1;
	private FTPTransferType ftt = null;
	private FTPWork work;
	private File f;
	private boolean cl = false;
	protected boolean hold = false;
	private long holdTimeout = 0L;
	
	public void cancel() {
		cl = true;
	}
	
	public void setType(FTPTransferType ftt, File f) {
		this.ftt = ftt;
		this.f = f;
		this.st = true;
	}
	
	private boolean st = false;
	private HostFTP host;
	
	public ThreadPassive(HostFTP host, FTPWork work, String ip, int port) {
		this.serv = ip;
		this.ep = port;
		this.work = work;
	}
	
	public ThreadPassive(HostFTP host, FTPWork work, ServerSocket serv) {
		this.serv = serv;
		this.work = work;
	}
	
	public void run() {
		try {
			while (!st) {
				if (cl) return;
				try {
					Thread.sleep(5L);
				}catch (InterruptedException e) {
					host.logger.logError(e);
				}
			}
			if (cl) return;
			Socket s = null;
			if (serv instanceof ServerSocket) {
				do {
					if (s != null) s.close();
					s = ((ServerSocket) serv).accept();
				}while (!s.getInetAddress().getHostAddress().equals(work.s.getInetAddress().getHostAddress()));
			}else {
				s = new Socket((String) serv, ep);
			}
			DataOutputStream out = new DataOutputStream(s.getOutputStream());
			out.flush();
			DataInputStream in = new DataInputStream(s.getInputStream());
			try {
				hold = true;
				if (work.s instanceof UNIOSocket) {
					((UNIOSocket) work.s).setHoldTimeout(true);
				}
				if (ftt == FTPTransferType.STOR || ftt == FTPTransferType.STOU || ftt == FTPTransferType.APPE) {
					work.writeLine(150, (ftt == FTPTransferType.STOU ? "FILE: " + FTPHandler.chroot(work.root, f) : "Ok to send data."));
					FileOutputStream fout = new FileOutputStream(f, ftt == FTPTransferType.APPE);
					byte[] buf = new byte[4096];
					int i;
					while (!s.isClosed()) {
						try {
							holdTimeout = 0L;
							i = in.read(buf);
							if (i < 0) {
								s.close();
								break;
							}else if (i == 0) {
								try {
									Thread.sleep(10L);
								}catch (InterruptedException e) {
									host.logger.logError(e);
								}
								continue;
							}
							fout.write(buf, 0, i);
							fout.flush();
						}catch (SocketTimeoutException e) {
							long now = System.currentTimeMillis();
							if (holdTimeout == 0L) {
								holdTimeout = now + 10000L;
							}else {
								if (now >= holdTimeout) {
									s.close();
									fout.flush();
									fout.close();
									return;
								}
							}
						}
					}
					fout.close();
					work.writeLine(226, "Transfer complete.");
				}else if (ftt == FTPTransferType.LIST || ftt == FTPTransferType.NLST) {
					if (f.isFile()) f = f.getParentFile();
					work.writeLine(150, "Ok Here comes the directory listing.");
					if (AvunaHTTPD.windows || ftt == FTPTransferType.NLST) {
						for (File sf : f.listFiles()) {
							out.write((sf.getName() + AvunaHTTPD.crlf).getBytes());
							out.flush();
						}
					}else {
						ProcessBuilder lspb = new ProcessBuilder("ls", "-lALh", f.getAbsolutePath()); // TODO: java impl
						Process ls = lspb.start(); // shell injection secure, as processbuilder is used
						try {
							ls.waitFor();
						}catch (InterruptedException e) {
							host.logger.logError(e);
						}
						Scanner in2 = new Scanner(ls.getInputStream());
						while (in2.hasNextLine()) {
							out.write((in2.nextLine() + AvunaHTTPD.crlf).getBytes());
							out.flush();
						}
						in2.close();
					}
					work.writeLine(226, "Directory send OK.");
				}else if (ftt == FTPTransferType.RETR) {
					work.writeLine(150, "Opening " + (work.type == FTPType.ASCII ? "ASCII" : "BINARY") + " mode data connection for " + f.getName() + ".");
					FileInputStream fin = new FileInputStream(f);
					if (work.skip > 0) {
						fin.skip(work.skip);
						work.skip = 0;
					}
					byte[] buf = new byte[1024];
					int i = 1;
					while (!s.isClosed() && i > 0) {
						i = fin.read(buf);
						if (i > 0) {
							out.write(buf, 0, i);
							out.flush();
						}
					}
					fin.close();
					work.writeLine(226, "Transfer complete.");
				}
				s.close();
			}finally {
				hold = false;
				if (work.s instanceof UNIOSocket) {
					((UNIOSocket) work.s).setHoldTimeout(false);
				}
			}
		}catch (IOException e) {
			if (!(e instanceof SocketException)) host.logger.logError(e);
			try {
				work.writeLine(526, "Transfer failed.");
			}catch (IOException e1) {
				host.logger.logError(e);
			}
		}finally {
			if (!cl) {
				work.isPASV = false;
				work.isPORT = false;
				work.psv = null;
			}
			try {
				if (serv instanceof ServerSocket) {
					((ServerSocket) serv).close();
				}
			}catch (IOException e) {
				host.logger.logError(e);
			}
		}
	}
}
