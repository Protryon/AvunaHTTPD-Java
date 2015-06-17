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

package org.avuna.httpd.http.plugins.javaloader.lib;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.plugins.javaloader.HTMLBuilder;

public class HTMLCache {
	private HashMap<String, String> html = new HashMap<String, String>();
	private File f;
	private static final ArrayList<HTMLCache> caches = new ArrayList<HTMLCache>();
	
	public static void reloadAll() throws IOException {
		for (HTMLCache cache : caches) {
			cache.reload();
		}
	}
	
	public void reload() throws IOException {
		html.clear();
		Scanner s = new Scanner(f);
		boolean on = false;
		int ll = 0;
		String name = "";
		StringBuilder cur = new StringBuilder();
		while (s.hasNextLine()) {
			String line = s.nextLine().trim();
			if (!on) {
				String[] spl = line.split(":");
				if (spl.length != 2) {
					continue;
				}
				name = spl[0];
				ll = Integer.parseInt(spl[1]);
				on = true;
			}else if (ll-- > 0) {
				cur.append(line + AvunaHTTPD.crlf);
			}else {
				on = false;
				html.put(name, cur.toString());
				cur = new StringBuilder();
				String[] spl = line.split(":");
				if (spl.length != 2) {
					continue;
				}
				name = spl[0];
				ll = Integer.parseInt(spl[1]);
				on = true;
			}
		}
		if (on) {
			on = false;
			html.put(name, cur.toString());
		}
		s.close();
	}
	
	public HTMLCache(File f) throws IOException {
		this.f = f;
		Scanner s = new Scanner(f);
		boolean on = false;
		int ll = 0;
		String name = "";
		StringBuilder cur = new StringBuilder();
		while (s.hasNextLine()) {
			String line = s.nextLine().trim();
			if (!on) {
				String[] spl = line.split(":");
				if (spl.length != 2) {
					continue;
				}
				name = spl[0];
				ll = Integer.parseInt(spl[1]);
				on = true;
			}else if (ll-- > 0) {
				cur.append(line + AvunaHTTPD.crlf);
			}else {
				on = false;
				html.put(name, cur.toString());
				cur = new StringBuilder();
				String[] spl = line.split(":");
				if (spl.length != 2) {
					continue;
				}
				name = spl[0];
				ll = Integer.parseInt(spl[1]);
				on = true;
			}
		}
		if (on) {
			on = false;
			html.put(name, cur.toString());
		}
		s.close();
		caches.add(this);
	}
	
	public String get(String name) {
		return html.get(name);
	}
	
	public void write(HTMLBuilder out, String name) throws IOException {
		out.println(html.get(name));
	}
}
