package org.avuna.httpd.dns.zone;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.avuna.httpd.dns.Type;
import org.avuna.httpd.dns.Util;

public class DNSRecord implements IDirective {
	private final String domain;
	private final Type type;
	private final int ttl;
	private final byte[] data;
	private final String[] tv;
	
	public String getDomain() {
		return domain;
	}
	
	public Type getType() {
		return type;
	}
	
	public int getTimeToLive() {
		return ttl;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public String[] getTV() {
		return tv;
	}
	
	public static byte[] getDataFromArgs(Type type, String[] args) throws IOException {
		byte[] fd = null;
		if (type == Type.A) {
			fd = Util.ipToByte(args[0]);
		}else if (type == Type.AAAA) {
			fd = Util.ip6ToByte(args[0]);
		}else if (type == Type.MX) {
			if (args.length != 2) {
				return null;
			}
			int priority = 0;
			try {
				Integer.parseInt(args[0]);
			}catch (NumberFormatException e) {
				return null;
			}
			ByteArrayOutputStream mxf = new ByteArrayOutputStream();
			DataOutputStream mxfd = new DataOutputStream(mxf);
			mxfd.writeShort(priority);
			String[] dspl = args[1].split("\\.");
			for (String d : dspl) {
				if (d.length() == 0) continue;
				mxfd.write(d.length());
				mxfd.write(d.getBytes());
			}
			mxfd.write(0);
			fd = mxf.toByteArray();
		}else if (type == Type.PTR || type == Type.CNAME || type == Type.DNAME || type == Type.NS) {
			ByteArrayOutputStream mxf = new ByteArrayOutputStream();
			DataOutputStream mxfd = new DataOutputStream(mxf);
			String[] dspl = args[0].split("\\.");
			for (String d : dspl) {
				if (d.length() == 0) continue;
				mxfd.write(d.length());
				mxfd.write(d.getBytes());
			}
			mxfd.write(0);
			fd = mxf.toByteArray();
		}else if (type == Type.TXT) {
			byte[] db = args[0].getBytes();
			fd = new byte[db.length + 1];
			fd[0] = (byte)db.length;
			System.arraycopy(db, 0, fd, 1, db.length);
		}else {
			fd = args[0].getBytes(); // TODO: ???
		}
		return fd;
	}
	
	public DNSRecord(String domain, Type type, int ttl, byte[] data, String[] tv) {
		this.domain = domain;
		this.type = type;
		this.ttl = ttl;
		this.data = data;
		this.tv = tv;
	}
	
	public DNSRecord(String domain, String ip, int ttl) {
		this.domain = domain;
		this.type = Type.A;
		this.ttl = ttl;
		this.data = Util.ipToByte(ip);
		this.tv = new String[]{ip};
	}
}
