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

package org.avuna.httpd.http2.networking;

public enum Flag {
	END_STREAM(1), END_HEADERS(4), PADDED(8), PRIORITY(32);
	
	public final int id;
	
	private Flag(int id) {
		this.id = id;
	}
	
	public static Flag getByID(int id) {
		for (Flag type : values()) {
			if (type.id == id) return type;
		}
		return null;
	}
}
