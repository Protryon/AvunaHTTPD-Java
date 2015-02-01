package com.javaprophet.javawebserver.networking;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPOutputStream;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.networking.packets.RequestPacket;
import com.javaprophet.javawebserver.networking.packets.ResponsePacket;

public class ThreadStreamWorker extends Thread {
	private final Work work;
	private final RequestPacket req;
	private final ResponsePacket resp;
	
	public ThreadStreamWorker(Work work, RequestPacket req, ResponsePacket resp) {
		this.work = work;
		this.req = req;
		this.resp = resp;
	}
	
	public void run() {
		try {
			boolean gzip = resp.headers.hasHeader("Content-Encoding") && resp.headers.getHeader("Content-Encoding").contains("gzip");
			PrintStream ps = new PrintStream(work.out);
			FileInputStream fin = new FileInputStream(JavaWebServer.fileManager.getAbsolutePath(resp.body.getBody().loc));
			int i = 0;
			byte[] buf = new byte[10485760];
			ByteArrayOutputStream bout = null;
			GZIPOutputStream gout = null;
			if (gzip) {
				bout = new ByteArrayOutputStream();
				gout = new GZIPOutputStream(bout);
			}
			// resp.headers.removeHeaders("Content-Encoding");
			while (!work.s.isClosed()) {
				i = fin.read(buf);
				if (i == -1) {
					work.s.close();
					return;
				}
				if (gzip) {
					gout.write(buf, 0, i);
					ByteBuffer bb = ByteBuffer.allocate(4);
					bb.putInt(0, bout.size());
					byte[] bas = new byte[4];
					bb.position(0);
					bb.get(bas);
					ps.println(JavaWebServer.fileManager.bytesToHex(bas));
					ps.write(bout.toByteArray());
					bout.reset();
				}else {
					ByteBuffer bb = ByteBuffer.allocate(4);
					bb.putInt(0, i);
					byte[] bas = new byte[4];
					bb.position(0);
					bb.get(bas);
					ps.println(JavaWebServer.fileManager.bytesToHex(bas));
					ps.write(buf, 0, i);
				}
				ps.println();
				ps.flush();
			}
			if (gzip) {
				gout.flush();
				gout.close();
				ByteBuffer bb = ByteBuffer.allocate(4);
				bb.putInt(0, bout.size());
				byte[] bas = new byte[4];
				bb.position(0);
				bb.get(bas);
				ps.println(JavaWebServer.fileManager.bytesToHex(bas));
				ps.write(bout.toByteArray());
				bout.reset();
				ps.println();
				ps.flush();
			}
			ps.println("0");
			ps.flush();
			ThreadWorker.readdWork(work);
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
}
