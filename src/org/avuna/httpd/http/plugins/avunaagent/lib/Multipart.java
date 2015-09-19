/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.avunaagent.lib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.avuna.httpd.util.Stream;
import org.avuna.httpd.util.logging.Logger;

public class Multipart {
	public final ArrayList<MultiPartData> mpds = new ArrayList<MultiPartData>();
	public String boundary;
	public String mct = "";
	
	/** @param sct Only used for mail, can be null or ignored. */
	public Multipart(Logger logger, String sct, byte[] content) {
		this(logger, sct, new ByteArrayInputStream(content));
	}
	
	/** @param sct Only used for mail, can be null or ignored. */
	public Multipart(Logger logger, String sct, InputStream bin) {
		this(logger, sct, null, bin);
	}
	
	public byte[] serialize(Logger logger) {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PrintStream pw = new PrintStream(bout);
		pw.println("--" + boundary);
		for (int i = 0; i < mpds.size(); i++) {
			MultiPartData mpd = mpds.get(i);
			if (mpd.contentType.trim().length() > 0) pw.println("Content-Type: " + mpd.contentType.trim());
			if (mpd.contentTransferEncoding.trim().length() > 0) pw.println("Content-Transfer-Encoding: " + mpd.contentTransferEncoding.trim());
			for (String h : mpd.extraHeaders) {
				pw.println(h);
			}
			pw.println();
			try {
				bout.write(mpd.data);
			}catch (IOException e) {
				logger.logError(e);
			}
			pw.println("--" + boundary + ((i == (mpds.size() - 1)) ? "--" : ""));
		}
		return bout.toByteArray();
	}
	
	/** @param sct Only used for mail, can be null or ignored. */
	public Multipart(Logger logger, String sct, String boundary, InputStream bin) {
		this.mct = sct;
		try {
			if (boundary == null) {
				do {
					
					this.boundary = Stream.readLine(bin);
				}while (!this.boundary.startsWith("-"));
			}else {
				this.boundary = boundary;
			}
			boolean hc = true;
			int mi = 0;
			while (Stream.readLine(bin).equals("")) {
			
			}
			while (hc && mi++ < 50) {
				String ct = "";
				HashMap<String, String> vars = new HashMap<String, String>();
				String contentTransferEncoding = "";
				String rawdisp = "";
				String str;
				ArrayList<String> extraHeaders = new ArrayList<String>();
				String ln = null;
				while (true) {
					str = Stream.readLine(bin);
					if (str == null || str.length() == 0) break;
					if (str.startsWith(" ")) {
						if (ln.equals("content-type")) {
							ct += str;
						}else if (ln.equals("content-disposition")) {
							rawdisp += str;
							String[] spl = str.split(";"); // TODO: wont allow midline breaking
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
						}else if (ln.equals("content-transfer-encoding")) {
							contentTransferEncoding += str;
						}else if (str.trim().length() > 0 && extraHeaders.size() > 0) {
							extraHeaders.set(extraHeaders.size() - 1, extraHeaders.get(extraHeaders.size() - 1) + str);
						}
						continue;
					}
					String name = str.substring(0, str.indexOf(":"));
					ln = name;
					String value = str.substring(name.length() + 1).trim();
					name = name.trim().toLowerCase();
					if (name.equals("content-type")) {
						ct = value;
					}else if (name.equals("content-disposition")) {
						rawdisp = value;
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
					}else if (str.trim().length() > 0) {
						extraHeaders.add(str.trim());
					}
				}
				byte[] data = readUntil(this.boundary.getBytes(), bin);
				if (data.length == 0) {
					hc = false;
					continue;
				}
				int clip = 0;
				for (int i = data.length - 1; i > 0; i--) {
					if (data[i] == '-') {
						clip++;
					}else {
						break;
					}
				}
				if (clip > 0) {
					byte[] cdata = new byte[data.length - clip];
					System.arraycopy(data, 0, cdata, 0, cdata.length);
					data = cdata;
				}
				byte[] nloe = new byte[2];
				bin.read(nloe);
				if (nloe[0] == 0x2D && nloe[1] == 0x2D) {
					hc = false;
				}
				mpds.add(new MultiPartData(ct, contentTransferEncoding, rawdisp, extraHeaders, vars, data));
			}
		}catch (IOException e) {
			logger.logError(e);
		}
	}
	
	private static byte[] readUntil(byte[] match, InputStream in) throws IOException {
		ByteArrayOutputStream writer = new ByteArrayOutputStream();
		int i = 0;
		int ml = 0;
		while ((i = in.read()) != -1) {
			if (i == match[ml]) {
				if (ml == 1 && in.markSupported()) {
					in.mark(match.length);
				}
				ml++;
				if (ml == match.length) {
					break;
				}
			}else if (ml > 0) {
				writer.write(match, 0, ml);
				writer.write(i);
				if (in.markSupported() && ml > 1) in.reset();
				ml = 0;
			}else {
				writer.write(i);
			}
		}
		return writer.toByteArray();
	}
	
	public static class MultiPartData {
		public final String contentType, contentTransferEncoding;
		public final HashMap<String, String> vars;
		public final byte[] data;
		public final String rawdisp;
		public final ArrayList<String> extraHeaders;
		
		private MultiPartData(String ct, String contentTransferEncoding, String rawdisp, ArrayList<String> extraHeaders, HashMap<String, String> vars, byte[] data) {
			this.contentType = ct;
			this.extraHeaders = extraHeaders;
			this.rawdisp = rawdisp;
			this.vars = vars;
			this.data = data;
			this.contentTransferEncoding = contentTransferEncoding;
		}
	}
}
