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

package org.avuna.httpd.dns;

public enum Type {
	A("A", 1), NS("NS", 2), CNAME("CNAME", 5), SOA("SOA", 6), PTR("PTR", 12), MX("MX", 15), TXT("TXT", 16), AAAA("AAAA", 28), SRV("SRV", 33), DNAME("DNAME", 39);
	
	public final String name;
	public final int id;
	
	private Type(String name, int id) {
		this.name = name;
		this.id = id;
	}
	
	public static Type getType(int id) {
		for (Type t : values()) {
			if (t.id == id) {
				return t;
			}
		}
		return null;
	}
	
	public static Type getType(String name) {
		for (Type t : values()) {
			if (t.name.equals(name)) {
				return t;
			}
		}
		return null;
	}
	
	public boolean matches(int id) {
		if (id == this.id) return true;
		if (id == 255) return true;
		return false;
	}
}
