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

package org.avuna.httpd.http.plugins.base.fcgi;


public enum Type {
	FCGI_BEGIN_REQUEST(1), FCGI_ABORT_REQUEST(2), FCGI_END_REQUEST(3), FCGI_PARAMS(4), FCGI_STDIN(5), FCGI_STDOUT(6), FCGI_STDERR(7), FCGI_DATA(8), FCGI_GET_VALUES(9), FCGI_GET_VALUES_RESULT(10), FCGI_UNKCOWN_TYPE(11), FCGI_MAXTYPE(-1);
	public final int id;
	
	private Type(int id) {
		this.id = id;
	}
	
	public static Type fromID(int id) {
		for (Type type : values()) {
			if (type.id == id) {
				return type;
			}
		}
		return FCGI_MAXTYPE;
	}
}
