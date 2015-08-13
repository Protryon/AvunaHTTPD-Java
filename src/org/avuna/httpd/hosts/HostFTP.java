/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.hosts;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.avuna.httpd.ftp.FTPAccountProvider;
import org.avuna.httpd.ftp.FTPConfigAccountProvider;
import org.avuna.httpd.ftp.FTPHandler;
import org.avuna.httpd.ftp.FTPPacketReceiver;
import org.avuna.httpd.ftp.FTPWork;
import org.avuna.httpd.ftp.ThreadAcceptFTP;
import org.avuna.httpd.ftp.ThreadWorkerFTP;
import org.avuna.httpd.ftp.ThreadWorkerUNIO;
import org.avuna.httpd.util.ConfigNode;
import org.avuna.httpd.util.unio.PacketReceiver;
import org.avuna.httpd.util.unio.UNIOSocket;

public class HostFTP extends Host {
	
	private int tac, twc, mc;
	public final FTPHandler ftphandler = new FTPHandler(this);
	public FTPAccountProvider provider;
	
	public HostFTP(String name) {
		this(name, null);
	}
	
	public HostFTP(String name, FTPAccountProvider provider) {
		super(name, Protocol.FTP);
		this.provider = provider;
	}
	
	public List<FTPWork> works = Collections.synchronizedList(new ArrayList<FTPWork>());
	public final ArrayList<ThreadWorkerFTP> workers = new ArrayList<ThreadWorkerFTP>();
	
	public int workSize() {
		return works.size();
	}
	
	private volatile int ci = 0;
	
	public void addWork(Socket s, DataInputStream in, DataOutputStream out) {
		if (unio()) {
			UNIOSocket us = (UNIOSocket) s;
			((ThreadWorkerUNIO) conns.get(ci)).poller.addSocket(us);
			ci++;
			if (ci == conns.size()) ci = 0;
		}
		works.add(new FTPWork(s, in, out, this));
	}
	
	public void setupFolders() {}
	
	public static void unpack() {}
	
	public void formatConfig(ConfigNode map) {
		if (!map.containsNode("port")) map.insertNode("port", "21");
		super.formatConfig(map);
		map.removeNode("ssl");
		if (!map.containsNode("acceptThreadCount")) map.insertNode("acceptThreadCount", "2", "accept thread count");
		if (!map.containsNode("workerThreadCount")) map.insertNode("workerThreadCount", "8", "worker thread count");
		if (!map.containsNode("maxConnections")) map.insertNode("maxConnections", "-1", "max connections per port");
		if (provider == null) {
			if (!map.containsNode("accounts")) map.insertNode("accounts");
			provider = new FTPConfigAccountProvider(this, map.getNode("accounts"));
		}
		tac = Integer.parseInt(map.getNode("acceptThreadCount").getValue());
		twc = Integer.parseInt(map.getNode("workerThreadCount").getValue());
		mc = Integer.parseInt(map.getNode("maxConnections").getValue());
	}
	
	public ArrayList<ThreadWorkerUNIO> conns = new ArrayList<ThreadWorkerUNIO>();
	
	public FTPWork getWork() {
		if (unio()) return null;
		synchronized (works) {
			for (int i = 0; i < works.size(); i++) {
				FTPWork work = works.get(i);
				if (work.inUse) continue;
				try {
					if (work.s.isClosed()) {
						work.close();
						i--;
						continue;
					}else {
						if (work.in.available() > 0) {
							work.inUse = true;
							return work;
						}
					}
				}catch (IOException e) {
					work.inUse = true;
					return work;
				}
			}
		}
		return null;
	}
	
	public boolean enableUNIO() {
		return true;
	}
	
	public PacketReceiver makeReceiver() {
		return new FTPPacketReceiver();
	}
	
	@Override
	public void setup(ServerSocket s) {
		for (int i = 0; i < tac; i++) {
			new ThreadAcceptFTP(this, s, mc).start();
		}
		if (unio()) for (int i = 0; i < twc; i++) {
			ThreadWorkerUNIO twf = new ThreadWorkerUNIO(this);
			addTerm(twf);
			twf.start();
		}
		else for (int i = 0; i < twc; i++) {
			ThreadWorkerFTP twf = new ThreadWorkerFTP(this);
			addTerm(twf);
			twf.start();
		}
	}
}
