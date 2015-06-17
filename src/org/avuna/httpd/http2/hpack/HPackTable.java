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

package org.avuna.httpd.http2.hpack;

import java.util.ArrayList;

public class HPackTable {
	public static Header[] staticTable = new Header[]{new Header("null", "null"), //
			new Header(":authority", ""),//
			new Header(":method", "GET"),//
			new Header(":method", "POST"),//
			new Header(":path", "/"),//
			new Header(":path", "/index.html"),//
			new Header(":scheme", "http"),//
			new Header(":scheme", "https"),//
			new Header(":status", "200"),//
			new Header(":status", "204"),//
			new Header(":status", "206"),//
			new Header(":status", "304"),//
			new Header(":status", "400"),//
			new Header(":status", "404"),//
			new Header(":status", "500"),//
			new Header("accept-charset", ""),//
			new Header("accept-encoding", "gzip, deflate"),//
			new Header("accept-language", ""),//
			new Header("accept-ranges", ""),//
			new Header("accept", ""),//
			new Header("access-control-allow-origin", ""),//
			new Header("age", ""),//
			new Header("allow", ""),//
			new Header("authorization", ""),//
			new Header("cache-control", ""),//
			new Header("content-disposition", ""),//
			new Header("content-encoding", ""),//
			new Header("content-language", ""),//
			new Header("content-length", ""),//
			new Header("content-location", ""),//
			new Header("content-range", ""),//
			new Header("content-type", ""),//
			new Header("cookie", ""),//
			new Header("date", ""),//
			new Header("etag", ""),//
			new Header("expect", ""),//
			new Header("expires", ""),//
			new Header("from", ""),//
			new Header("host", ""),//
			new Header("if-match", ""),//
			new Header("if-modified-since", ""),//
			new Header("if-none-match", ""),//
			new Header("if-range", ""),//
			new Header("if-unmodified-since", ""),//
			new Header("last-modified", ""),//
			new Header("link", ""),//
			new Header("location", ""),//
			new Header("max-forwards", ""),//
			new Header("proxy-authenticate", ""),//
			new Header("proxy-authorization", ""),//
			new Header("range", ""),//
			new Header("referer", ""),//
			new Header("refresh", ""),//
			new Header("retry-after", ""),//
			new Header("server", ""),//
			new Header("set-cookie", ""),//
			new Header("strict-transport-security", ""),//
			new Header("transfer-encoding", ""),//
			new Header("user-agent", ""),//
			new Header("vary", ""),//
			new Header("via", ""),//
			new Header("www-authenticate", ""),//
	};
	private int maxSize = 0;
	private ArrayList<Header> dynamicList = new ArrayList<Header>();
	
	public HPackTable(int maxSize) {
		this.maxSize = maxSize;
	}
	
	private int currentSize() {
		int s = 0;
		for (Header header : dynamicList) {
			s += header.sizeHPACK();
		}
		return s;
	}
	
	public void addEntry(Header header) {
		while (currentSize() + header.sizeHPACK() > maxSize) {
			dynamicList.remove(0);
		}
		dynamicList.add(header);
	}
	
	public Header getEntry(int i) {
		if (i < staticTable.length) {
			return staticTable[i];
		}else {
			return dynamicList.get(i - staticTable.length);
		}
	}
	
}
