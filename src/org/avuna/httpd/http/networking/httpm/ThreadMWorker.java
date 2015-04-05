package org.avuna.httpd.http.networking.httpm;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.hosts.VHostM;
import org.avuna.httpd.http.Resource;
import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;
import org.avuna.httpd.http.networking.ThreadWorker;
import org.avuna.httpd.util.Logger;

public class ThreadMWorker extends ThreadWorker {
	
	public ThreadMWorker(HostHTTP host) {
		super(host);
		this.setName("Avuna HTTPM-Worker Thread #" + (nid - 1));
	}
	
	private static String readLine(DataInputStream in) throws IOException {
		ByteArrayOutputStream writer = new ByteArrayOutputStream();
		int i = in.read();
		if (i == -1) return null;
		while (i != AvunaHTTPD.crlfb[0] && i != -1) {
			writer.write(i);
			i = in.read();
		}
		if (AvunaHTTPD.crlfb.length == 2) in.read();
		return writer.toString();
	}
	
	public void run() {
		while (keepRunning) {
			RequestPacket incomingRequest = host.pollReqQueue();
			if (incomingRequest == null) {
				try {
					Thread.sleep(1L);
				}catch (InterruptedException e) {
					Logger.logError(e);
				}
				continue;
			}
			try {
				if (!(incomingRequest.host instanceof VHostM)) {
					continue;
				}
				VHostM vh = (VHostM)incomingRequest.host;
				long benchStart = System.nanoTime();
				if (incomingRequest.work.cn == null) {
					Socket s = new Socket(vh.ip, vh.port);
					DataOutputStream out = new DataOutputStream(s.getOutputStream());
					out.flush();
					DataInputStream in = new DataInputStream(s.getInputStream()); // TODO: auth
					incomingRequest.work.cn = new MasterConn(s, out, in);
				}
				long est = 0L, writ = 0L, hdr = 0L;
				try {
					est = System.nanoTime();
					incomingRequest.write(incomingRequest.work.cn.out);
					incomingRequest.work.cn.out.flush();
					writ = System.nanoTime();
					ResponsePacket outgoingResponse = incomingRequest.child;
					outgoingResponse.request = incomingRequest;
					String line = readLine(incomingRequest.work.cn.in);
					if (line == null) {
						VHostM vm = (VHostM)incomingRequest.host;
						Socket s = new Socket(vm.ip, vm.port);
						DataOutputStream out = new DataOutputStream(s.getOutputStream());
						out.flush();
						DataInputStream in = new DataInputStream(s.getInputStream());
						incomingRequest.work.cn = new MasterConn(s, out, in);
						est = System.nanoTime();
						incomingRequest.write(incomingRequest.work.cn.out);
						incomingRequest.work.cn.out.flush();
						writ = System.nanoTime();
						line = readLine(incomingRequest.work.cn.in);
						if (line == null) {
							Logger.log("Reconnect failed, check VHost connection!");
						}
					}
					int i = line.indexOf(" ");
					outgoingResponse.httpVersion = line.substring(0, i);
					i++;
					outgoingResponse.statusCode = Integer.parseInt(line.substring(i, (i = line.indexOf(" ", i))));
					i++;
					outgoingResponse.reasonPhrase = line.substring(i);
					while ((line = readLine(incomingRequest.work.cn.in)).length() > 0) {
						outgoingResponse.headers.addHeader(line);
					}
					hdr = System.nanoTime();
					
					if (outgoingResponse.headers.hasHeader("Content-Length")) {
						byte[] data = new byte[Integer.parseInt(outgoingResponse.headers.getHeader("Content-Length"))];
						incomingRequest.work.cn.in.readFully(data);
						readLine(incomingRequest.work.cn.in);
						outgoingResponse.body = new Resource(data, outgoingResponse.headers.hasHeader("Content-Type") ? outgoingResponse.headers.getHeader("Content-Type") : "text/html; charset=utf-8");
						outgoingResponse.prewrite();
						outgoingResponse.done = true;
						outgoingResponse.bwt = System.nanoTime();
					}else {
						Logger.log("VHost streaming not yet supported!");
					}
				}catch (Exception e) {
					Logger.logError(e);
				}finally {
				}
				long cls = System.nanoTime();
				// Logger.log((est - benchStart) / 1000000D + " start-est");
				// Logger.log((writ - est) / 1000000D + " est-writ");
				// Logger.log((hdr - writ) / 1000000D + " writ-hdr");
				// Logger.log((incomingRequest.child.bwt - hdr) / 1000000D + " hdr-bwt");
				// Logger.log((cls - incomingRequest.child.bwt) / 1000000D + " bwt-cls");
				if (incomingRequest.host.getDebug()) {
					Logger.log(AvunaHTTPD.crlf + incomingRequest.toString().trim());
				}
				Logger.log(incomingRequest.userIP + " " + incomingRequest.method.name + " " + incomingRequest.host.getHostPath() + incomingRequest.target + " returned " + incomingRequest.child.statusCode + " " + incomingRequest.child.reasonPhrase + " took: " + (incomingRequest.child.bwt - benchStart) / 1000000D + " ms");
			}catch (Exception e) {
				if (!(e instanceof SocketException || e instanceof StringIndexOutOfBoundsException)) {
					Logger.logError(e);
					
				}else {
					try {
						incomingRequest.work.s.close();
					}catch (IOException ex) {
						Logger.logError(ex);
					}
				}
			}finally {
				if (host.sizeReqQueue() < 10000) { // idle fix
					try {
						Thread.sleep(0L, 100000);
					}catch (InterruptedException e) {
						Logger.logError(e);
					}
				}
			}
		}
	}
}
