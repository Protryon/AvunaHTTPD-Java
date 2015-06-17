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

package org.avuna.httpd.http.event;

public abstract class HTTPEventID {
	public static final int METHODLOOKUP = 7;
	public static final int GENERATERESPONSE = 8;
	public static final int PREPROCESSREQUEST = 9;
	public static final int RESPONSEFINISHED = 10;
	public static final int RESPONSESENT = 11;
	public static final int CLEARCACHE = 12;
}
