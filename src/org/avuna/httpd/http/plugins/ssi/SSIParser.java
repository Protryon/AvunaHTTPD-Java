/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */
package org.avuna.httpd.http.plugins.ssi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

public class SSIParser {
	private SSIEngine engine;
	
	protected void setEngine(SSIEngine engine) {
		this.engine = engine;
	}
	
	private final Pattern ssiDirective = Pattern.compile("<!--\\s*#([a-zA-Z]*)\\s*(.*?)-->");
	
	private final HashMap<Long, ParsedSSIDirective[]> dirCache = new HashMap<Long, ParsedSSIDirective[]>();
	
	/** Clears the internal directive cache. */
	public void clearCache() {
		dirCache.clear();
	}
	
	/** Parses a page with standard SSI syntax into an array of ParsedSSIDirectives. */
	public Page parsePage(String page) {
		CRC32 crc32 = new CRC32();
		crc32.update(page.getBytes());
		ParsedSSIDirective[] dirs = null;
		long crc = crc32.getValue();
		if (dirCache.containsKey(crc)) {
			dirs = dirCache.get(crc);
		}else {
			Matcher m = ssiDirective.matcher(page);
			ArrayList<ParsedSSIDirective> ldirs = new ArrayList<ParsedSSIDirective>();
			while (m.find()) {
				String directive = m.group(1);
				String dargs = m.group(2);
				ArrayList<String> args = new ArrayList<String>();
				int sl = 0;
				int stage = 0;
				String cur = "";
				char quot = 0;
				while (sl < dargs.length()) {
					if (stage == 0) {
						int t = dargs.indexOf("=", sl);
						if (t < sl) break;
						cur = dargs.substring(sl, t + 1); // inc =
						sl += cur.length();
						cur = cur.trim();
						stage++;
					}else if (stage == 1) {
						boolean esc = false;
						sl = dargs.indexOf('"', sl) + 1; // skip ahead past next "
						int osl = sl;
						int tl = dargs.indexOf('\'', sl);
						if (tl >= 0) sl = Math.min(sl, tl + 1);
						if (osl != sl) { // single quote
							quot = '\'';
						}else {
							quot = '"';
						}
						int s = sl;
						while (sl < dargs.length()) {
							char c = dargs.charAt(sl);
							if (c == '\\') {
								esc = !esc;
							}else if (!esc) {
								if (c == quot) { // found unescaped terminator
									break;
								}
							}else {
								esc = false;
							}
							sl++;
						}
						cur += dargs.substring(s, sl); // name=value , no quotes
						args.add(cur);
						stage = 0;
					}
				}
				ParsedSSIDirective pd = new ParsedSSIDirective(directive, args.toArray(new String[0]), m.start(), m.end());
				ldirs.add(pd);
			}
			dirs = ldirs.toArray(new ParsedSSIDirective[0]);
			dirCache.put(crc, dirs);
		}
		return newPage(engine, dirs);
	}
	
	/** To be overridden for classes extending Page. */
	public Page newPage(SSIEngine engine, ParsedSSIDirective[] dirs) {
		return new Page(engine, dirs);
	}
}
