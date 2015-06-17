/*	Avuna HTTPD - General Server Applications
    Copyright (C) 2015 Maxwell Bruce

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package org.avuna.httpd.http.j2p;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.avuna.httpd.AvunaHTTPD;

public class JavaToPHP {
	public static void convert(String cname, PrintStream out, String inp) {
		String in = inp.replace("<?", "<?php").replace("<?phpphp", "<?php");// TODO: <?pHp, etc
		out.println("import java.io.PrintStream;");
		out.println("import com.javaprophet.javawebserver.networking.packets.RequestPacket;");
		out.println("import com.javaprophet.javawebserver.networking.packets.ResponsePacket;");
		out.println("import com.javaprophet.javawebserver.plugins.javaloader.JavaLoaderStream;");
		out.println();
		out.println("public class " + cname + " extends JavaLoaderPrint {");
		out.println("    public void generate(PrintStream out, ResponsePacket response, RequestPacket request) {");
		String[] phpspl = in.split("<\\?php");
		ArrayList<String> chunks = new ArrayList<String>();
		for (int i = 0; i < phpspl.length; i++) {
			if (i == 0) {
				chunks.add("HTML:" + phpspl[i]);
			}else {
				String pashp = phpspl[i].substring(0, phpspl[i].indexOf("?>"));
				String ahtml = ((phpspl[i].indexOf("?>") + 2 < phpspl[i].length()) ? phpspl[i].substring(phpspl[i].indexOf("?>") + 2) : ""); // TODO: ?> in quotes
				chunks.add("PHP:" + pashp);
				chunks.add("HTML:" + ahtml);
			}
		}
		for (String pchunk : chunks) {
			if (pchunk.startsWith("HTML:")) {
				String html = pchunk.substring(5);
				for (String line : html.split(AvunaHTTPD.crlf)) {
					out.println("        out.println(\"" + line.trim().replace("\\", "\\\\").replace("\"", "\\\"") + "\");");
				}
			}else {
				String php = pchunk.substring(4).trim();
				// System.out.println(php);
				Pattern pattern = Pattern.compile("\\$.*=.*;");
				Matcher matcher = pattern.matcher(new String(php));
				int al = 0;
				while (matcher.find()) {
					int start = matcher.start() + al;
					int end = matcher.end() + al;
					String vn = php.substring(start + 1, php.indexOf("=", start + 1)).trim();
					String vd = php.substring(php.indexOf("=", start + 1) + 1, end - 1).trim();
					al += ((11 + vn.length() + vd.length()) - (end - start));
					php = php.substring(0, start) + "Object " + vn + " = " + vd + ";" + php.substring(end);
				}
				for (String line : php.split(AvunaHTTPD.crlf)) {
					if (line.trim().equals("")) continue;
					out.println("        " + line.trim());
				}
			}
		}
		out.println("    }");
		out.println("}");
	}
}
