package org.avuna.httpd.mail.mailbox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.Headers;
import org.avuna.httpd.http.plugins.javaloader.lib.Multipart;
import org.avuna.httpd.util.Logger;

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
	
	public Email(String data, int uid, String from) {
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
					ArrayList<String> hds = headers.getHeaders(lhn);
					if (hds.size() > 0) {
						int i = hds.size() - 1;
						hds.set(i, hds.get(i) + AvunaHTTPD.crlf + line);
					}
				}
			}
		}catch (IOException e) {
			Logger.logError(e);
		}
		String ct = headers.getHeader("Content-Type");
		boolean mp = ct != null && ct.startsWith("multipart/alternative");
		byte[] body = new byte[bin.available()];
		if (mp) bin.mark(body.length);
		try {
			bin.read(body);
		}catch (IOException e) {
			Logger.logError(e);
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
			this.mp = new Multipart(b, bin);
		}else {
			this.mp = null;
		}
		this.uid = uid;
		this.from = from;
	}
}
