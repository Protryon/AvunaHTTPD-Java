package org.avuna.httpd.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.avuna.httpd.AvunaHTTPD;

public class Stream {
	public static String readLine(InputStream in) throws IOException {
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
}
