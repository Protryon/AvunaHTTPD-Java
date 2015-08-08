/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.mailbox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.http.Headers;
import org.avuna.httpd.http.plugins.avunaagent.lib.Multipart;

public class Email {
	public final ArrayList<String> flags = new ArrayList<String>();
	public String body;
	public final String data; // TODO: deprecate
	public final String from;
	public final ArrayList<String> to = new ArrayList<String>();
	public final Headers headers = new Headers();
	public int uid;
	public final Multipart mp;
	
	private static String readLine(InputStream in) throws IOException {
		ByteArrayOutputStream writer = new ByteArrayOutputStream();
		int i = in.read();
		while (i != AvunaHTTPD.crlfb[0] && i != -1) {
			writer.write(i);
			i = in.read();
		}
		if (AvunaHTTPD.crlfb.length == 2) in.read();
		return writer.toString();
	}
	
	public Email(HostMail host, String data, int uid, String from) {
		this.data = new String(data);
		String line = "";
		ByteArrayInputStream bin = new ByteArrayInputStream(data.getBytes());
		try {
			String lhn = "";
			while ((line = readLine(bin)).length() > 0) {
				if (line.contains(":")) {
					lhn = line.substring(0, line.indexOf(":"));
					String hd = line.substring(lhn.length() + 1).trim();
					headers.addHeader(lhn, hd);
				}else {
					if (lhn.trim().length() == 0) continue;
					String[] hds = headers.getHeaders(lhn);
					if (hds.length > 0) {
						int i = hds.length - 1;
						hds[i] = hds[i] + line;
					}
				}
			}
		}catch (IOException e) {
			host.logger.logError(e);
		}
		String ct = headers.getHeader("Content-Type");
		boolean mp = ct != null && ct.startsWith("multipart/");
		byte[] body = new byte[bin.available()];
		if (mp) bin.mark(body.length);
		try {
			bin.read(body);
		}catch (IOException e) {
			host.logger.logError(e);
		}
		this.body = new String(body);
		if (mp) {
			bin.reset();
			String b = null;
			if (ct.contains(";")) {
				ct = ct.substring(ct.indexOf(";") + 1).trim();
				if (ct.startsWith("boundary=")) {
					b = ct.substring(9, ct.contains(";") ? ct.indexOf(";") : ct.length());
					if (b.startsWith("\"") && b.endsWith("\"")) b = b.substring(1, b.length() - 1);
				}
			}
			this.mp = new Multipart(host.logger, ct, b, bin);
			// if (this.mp.mpds.size() > 0) {
			// MultiPartData mpd = this.mp.mpds.get(0);
			// if (mpd.contentType != null && mpd.contentType.equals("")) {
			// this.hasExtraPart = true;
			// }
			// }
		}else {
			this.mp = null;
		}
		this.uid = uid;
		this.from = from;
	}
}
