package org.avuna.httpd.http.networking;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.SocketException;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.util.Logger;

public class ThreadRawStreamWorker extends Thread {
	private final Work work;
	private final RequestPacket req;
	private final ResponsePacket resp;
	private final HostHTTP host;
	private final DataInputStream raw;
	
	public ThreadRawStreamWorker(HostHTTP host, Work work, RequestPacket req, ResponsePacket resp, DataInputStream raw) {
		this.work = work;
		this.req = req;
		this.resp = resp;
		this.host = host;
		this.raw = raw;
		this.setDaemon(true);
	}
	
	public void run() {
		// resp.headers.removeHeaders("Content-Encoding");
		try {
			ChunkedOutputStream cos = new ChunkedOutputStream(work.out, resp, false);
			ChunkedInputStream cis = new ChunkedInputStream(raw, false);
			cos.writeHeaders();
			while (!work.s.isClosed() && !cis.isEnded()) {
				int ba = cis.blockAvailable(true);
				byte[] buf = new byte[ba];
				cis.read(buf);
				cos.write(buf);
				cos.flush();
			}
			cos.finish();
			// cos.close();
			host.readdWork(work);
		}catch (IOException e) {
			if (!(e instanceof SocketException)) Logger.logError(e);
		}
	}
}
