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

package org.avuna.httpd.event.base;

public abstract class EventID {
	public static final int CONNECTED = 0;
	public static final int DISCONNECTED = 1;
	public static final int POSTINIT = 2;
	public static final int PREEXIT = 3;
	public static final int RELOAD = 4;
	public static final int SETUPFOLDERS = 5;
	public static final int PRECONNECT = 6;
}
