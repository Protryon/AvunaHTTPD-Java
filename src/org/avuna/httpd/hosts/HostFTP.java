package org.avuna.httpd.hosts;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import org.avuna.httpd.ftp.FTPHandler;
import org.avuna.httpd.ftp.FTPWork;
import org.avuna.httpd.ftp.ThreadAcceptFTP;
import org.avuna.httpd.ftp.ThreadWorkerFTP;
import org.avuna.httpd.util.ConfigNode;

public class HostFTP extends Host {
	
	private int tac, twc, mc;
	public final FTPHandler ftphandler = new FTPHandler(this);
	
	public HostFTP(String name) {
		super(name, Protocol.MAIL);
	}
	
	public void clearWork() {
		workQueue.clear();
	}
	
	public ArrayBlockingQueue<FTPWork> workQueue;
	public final ArrayList<ThreadWorkerFTP> workers = new ArrayList<ThreadWorkerFTP>();
	
	public void addWork(Socket s, DataInputStream in, DataOutputStream out) {
		workQueue.add(new FTPWork(s, in, out));
	}
	
	public int getQueueSizeSMTP() {
		return workQueue.size();
	}
	
	public void setupFolders() {
	}
	
	public static void unpack() {
	}
	
	public void formatConfig(ConfigNode map) {
		super.formatConfig(map);
		map.removeNode("ssl");
		if (!map.containsNode("acceptThreadCount")) map.insertNode("acceptThreadCount", "2", "accept thread count");
		if (!map.containsNode("workerThreadCount")) map.insertNode("workerThreadCount", "8", "worker thread count");
		if (!map.containsNode("maxConnections")) map.insertNode("maxConnections", "-1", "max connections per port");
		tac = Integer.parseInt(map.getNode("acceptThreadCount").getValue());
		twc = Integer.parseInt(map.getNode("workerThreadCount").getValue());
		mc = Integer.parseInt(map.getNode("maxConnections").getValue());
	}
	
	@Override
	public void setup(ServerSocket s) {
		workQueue = new ArrayBlockingQueue<FTPWork>(mc == -1 ? 1000000 : mc);
		for (int i = 0; i < tac; i++) {
			new ThreadAcceptFTP(this, s, mc).start();
		}
		for (int i = 0; i < twc; i++) {
			ThreadWorkerFTP twf = new ThreadWorkerFTP(this);
			addTerm(twf);
			twf.start();
		}
	}
}
