/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.avunaagent.lib;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class AssetLibrary {
	private HashMap<String, byte[]> data = new HashMap<String, byte[]>();
	private File f;
	private static final ArrayList<AssetLibrary> caches = new ArrayList<AssetLibrary>();
	private final boolean precache;
	
	public static void reloadAll() throws IOException {
		for (AssetLibrary cache : caches) {
			cache.reload();
		}
	}
	
	public void reload() throws IOException {
		data.clear();
		if (precache) recurPreload(f);
	}
	
	private void recurPreload(File f) throws IOException {
		for (File sf : f.listFiles()) {
			if (sf.isDirectory()) {
				recurPreload(sf);
			}else {
				FileInputStream fin = new FileInputStream(sf);
				byte[] buf = new byte[1024];
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				int i = 0;
				do {
					i = fin.read(buf);
					if (i > 0) bout.write(buf, 0, i);
				}while (i > 0);
				fin.close();
				String name = sf.getAbsolutePath().substring(this.f.getAbsolutePath().length());
				name = name.replace("\\", "/");
				if (name.startsWith("/")) name = name.substring(1);
				data.put(name, bout.toByteArray());
			}
		}
	}
	
	public AssetLibrary(File f, boolean precache) throws IOException {
		this.f = f;
		this.precache = precache;
		if (!f.isDirectory()) f.mkdirs();
		if (precache) reload();
		caches.add(this);
	}
	
	public byte[] get(String name) throws IOException {
		if (precache && !data.containsKey(name)) throw new IllegalArgumentException("Entry does not exist in Asset Library: " + name);
		if (data.containsKey(name)) return data.get(name);
		else {
			File sf = new File(f, name);
			if (!sf.exists()) {
				throw new IOException("Entry does not exist in Asset Library: " + name + " <" + sf.getAbsolutePath() + ">");
			}
			if (!sf.canRead()) {
				throw new IOException("Entry does not have permissions to read in Asset Library: " + name + " <" + sf.getAbsolutePath() + ">");
			}
			FileInputStream fin = new FileInputStream(sf);
			byte[] buf = new byte[1024];
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			int i = 0;
			do {
				i = fin.read(buf);
				if (i > 0) bout.write(buf, 0, i);
			}while (i > 0);
			fin.close();
			String fn = f.getAbsolutePath().substring(this.f.getAbsolutePath().length());
			fn = fn.replace("\\", "/");
			if (fn.startsWith("/")) fn = fn.substring(0);
			byte[] fb = bout.toByteArray();
			data.put(fn, fb);
			return fb;
		}
	}
	
	public String getString(String name) throws IOException {
		return new String(get(name));
	}
	
	public void write(PrintWriter out, String name) throws IOException {
		out.println(getString(name));
	}
}
