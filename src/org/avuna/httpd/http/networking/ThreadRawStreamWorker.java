/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.networking;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.SocketException;
import org.avuna.httpd.hosts.HostHTTP;
import org.avuna.httpd.util.unio.UNIOSocket;

public class ThreadRawStreamWorker extends Thread {
	private final Work work;
	private final ResponsePacket resp;
	private final HostHTTP host;
	private final DataInputStream raw;
	
	public ThreadRawStreamWorker(HostHTTP host, Work work, ResponsePacket resp, DataInputStream raw) {
		this.work = work;
		this.resp = resp;
		this.host = host;
		this.raw = raw;
		this.setDaemon(true);
	}
	
	public void run() {
		// resp.headers.removeHeaders("Content-Encoding");
		try {
			@SuppressWarnings("resource")
			ChunkedOutputStream cos = new ChunkedOutputStream(work.out, resp, false, work.s instanceof UNIOSocket ? (UNIOSocket) work.s : null);
			@SuppressWarnings("resource")
			ChunkedInputStream cis = new ChunkedInputStream(raw, false);
			cos.writeHeaders();
			while (!work.s.isClosed() && !cis.isEnded()) {
				int ba = cis.blockAvailable(true);
				if (ba < 0) break;
				byte[] buf = new byte[ba];
				cis.read(buf);
				cos.write(buf);
				cos.flush();
			}
			cos.finish();
			// cos.close();
			host.readdWork(work);
		}catch (IOException e) {
			if (!(e instanceof SocketException)) resp.request.host.logger.logError(e);
		}
	}
}
