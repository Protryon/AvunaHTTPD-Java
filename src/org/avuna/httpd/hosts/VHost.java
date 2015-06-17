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

package org.avuna.httpd.hosts;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import org.avuna.httpd.http.plugins.javaloader.JavaLoaderSession;

public class VHost {
	private final HostHTTP host;
	private final File htdocs, htsrc;
	private final String name, vhost;
	private JavaLoaderSession jls;
	private final VHost parent;
	private ArrayList<VHost> children = new ArrayList<VHost>();
	
	public VHost(String name, HostHTTP host, String vhost, VHost parent) {
		this.name = name;
		this.host = host;
		this.htdocs = parent.htdocs;
		this.htsrc = parent.htsrc;
		this.vhost = vhost;
		this.parent = parent;
		parent.children.add(this);
	}
	
	public VHost(String name, HostHTTP host, File htdocs, File htsrc, String vhost) {
		this.name = name;
		this.host = host;
		this.htdocs = htdocs;
		this.htsrc = htsrc;
		this.vhost = vhost;
		this.parent = null;
	}
	
	public boolean isChild() {
		return parent != null;
	}
	
	public void initJLS(URL[] url) {
		if (this.parent == null) {
			this.jls = new JavaLoaderSession(this, url);
			for (VHost child : children) {
				child.jls = this.jls;
			}
		}
	}
	
	public String getName() {
		return name;
	}
	
	private boolean debug = false;
	
	public void setDebug(boolean set) {
		debug = set;
	}
	
	public boolean getDebug() {
		return debug;
	}
	
	public JavaLoaderSession getJLS() {
		return jls;
	}
	
	public String getVHost() {
		return vhost;
	}
	
	public HostHTTP getHost() {
		return host;
	}
	
	public void setupFolders() {
		if (htdocs != null) htdocs.mkdirs();
		if (htsrc != null) htsrc.mkdirs();
	}
	
	public File getHTDocs() {
		return htdocs;
	}
	
	public File getHTSrc() {
		return htsrc;
	}
	
	public String getHostPath() {
		return name;
	}
}
