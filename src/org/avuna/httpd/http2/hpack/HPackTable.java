package org.avuna.httpd.http2.hpack;

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
}
