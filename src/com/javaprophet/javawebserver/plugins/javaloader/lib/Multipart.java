package com.javaprophet.javawebserver.plugins.javaloader.lib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import com.javaprophet.javawebserver.JavaWebServer;
import com.javaprophet.javawebserver.util.Logger;

public class Multipart {
	public final ArrayList<MultiPartData> mpds = new ArrayList<MultiPartData>();
	
	public Multipart(byte[] content) {
		ByteArrayInputStream bin = new ByteArrayInputStream(content);
		try {
			String sep = readLine(bin);
			boolean hc = true;
			String line;
			int mi = 0;
			while (hc && mi++ < 50) {
				String ct = "application/octet-stream";
				HashMap<String, String> vars = new HashMap<String, String>();
				String contentTransferEncoding = "";
				String str;
				boolean fh = false;
				while (true) {
					str = readLine(bin);
					if (str.length() == 0) {
						if (fh) break;
						else continue;
					}
					fh = true;
					String name = str.substring(0, str.indexOf(":"));
					String value = str.substring(name.length() + 1).trim();
					name = name.trim().toLowerCase();
					if (name.equals("content-type")) {
						ct = value;
					}else if (name.equals("content-disposition")) {
						String[] spl = value.split(";");
						// assume [0] is form-data;
						for (int i = 1; i < spl.length; i++) {
							String sn = spl[i].substring(0, spl[i].indexOf("="));
							String sv = spl[i].substring(sn.length() + 1).trim();
							sn = sn.trim();
							if (sv.startsWith("\"") && sv.endsWith("\"")) {
								sv = sv.substring(1, sv.length() - 1);
							}
							vars.put(sn, sv);
						}
					}else if (name.equals("content-transfer-encoding")) {
						contentTransferEncoding = value;
					}
				}
				byte[] data = readUntil(sep.getBytes(), bin);
				byte[] nloe = new byte[2];
				bin.read(nloe);
				if (nloe[0] == 0x2D && nloe[1] == 0x2D) {
					hc = false;
				}
				mpds.add(new MultiPartData(ct, contentTransferEncoding, vars, data));
			}
		}catch (IOException e) {
			Logger.logError(e);
		}
	}
	
	private static byte[] readUntil(byte[] match, InputStream in) throws IOException {
		ByteArrayOutputStream writer = new ByteArrayOutputStream();
		int i = 0;
		int ml = 0;
		while ((i = in.read()) != -1) {
			if (i == match[ml]) {
				ml++;
				if (ml == match.length) {
					break;
				}
			}else if (ml > 0) {
				writer.write(match, 0, ml);
				writer.write(i);
				ml = 0;
			}else {
				writer.write(i);
			}
		}
		return writer.toByteArray();
	}
	
	private static String readLine(InputStream in) throws IOException {
		ByteArrayOutputStream writer = new ByteArrayOutputStream();
		int i = in.read();
		while (i != JavaWebServer.crlfb[0] && i != -1) {
			writer.write(i);
			i = in.read();
		}
		if (JavaWebServer.crlfb.length == 2) in.read();
		return writer.toString();
	}
	
	public static class MultiPartData {
		public final String contentType, contentTransferEncoding;
		public final HashMap<String, String> vars;
		public final byte[] data;
		
		private MultiPartData(String ct, String contentTransferEncoding, HashMap<String, String> vars, byte[] data) {
			this.contentType = ct;
			this.vars = vars;
			this.data = data;
			this.contentTransferEncoding = contentTransferEncoding;
		}
	}
}
