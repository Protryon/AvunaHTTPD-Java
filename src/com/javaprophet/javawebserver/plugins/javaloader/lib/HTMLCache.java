package com.javaprophet.javawebserver.plugins.javaloader.lib;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import com.javaprophet.javawebserver.JavaWebServer;

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
				cur.append(line + JavaWebServer.crlf);
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
				cur.append(line + JavaWebServer.crlf);
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
	
	public void write(PrintStream out, String name) {
		out.println(html.get(name));
	}
}
