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

import java.util.ArrayList;

public class Protocol {
	private static final ArrayList<Protocol> protocols = new ArrayList<Protocol>();
	public static final Protocol HTTP = new Protocol("HTTP");
	public static final Protocol HTTPM = new Protocol("HTTPM");
	public static final Protocol MAIL = new Protocol("MAIL");
	public static final Protocol DNS = new Protocol("DNS");
	public static final Protocol COM = new Protocol("COM");
	public static final Protocol FTP = new Protocol("FTP");
	public final String name;
	
	public Protocol(String name) {
		this.name = name;
		protocols.add(this);
	}
	
	public static Protocol fromString(String s) {
		for (Protocol val : protocols) {
			if (val.name.equalsIgnoreCase(s)) return val;
		}
		return null;
	}
}
