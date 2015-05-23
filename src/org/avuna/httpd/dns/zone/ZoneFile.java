package org.avuna.httpd.dns.zone;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.dns.Type;
import org.avuna.httpd.dns.Util;
import org.avuna.httpd.util.Logger;

public class ZoneFile {
	private File f = null;
	private IDirective[] dirs = new IDirective[0];
	
	public IDirective[] getDirectives() {
		return dirs;
	}
	
	public ZoneFile(File f) {
		this.f = f;
	}
	
	private static void subload(File f, ArrayList<IDirective> dirs) throws IOException {
		int ttl = 3600;
		int ln = 0;
		Scanner s = new Scanner(f);
		while (s.hasNextLine()) {
			String line = s.nextLine().trim();
			String com = line.substring(0, line.contains(" ") ? line.indexOf(" ") : line.length());
			line = line.substring(com.length()).trim();
			String[] args = line.split(" ");
			if (args.length > 0) {
				String[] tcargs = new String[args.length];
				int nl = 0;
				boolean iq = false;
				String tmp = "";
				for (int i = 0; i < args.length; i++) {
					boolean niq = false;
					String ct = args[i].trim();
					if (!iq && ct.startsWith("\"")) {
						iq = true;
						niq = true;
					}
					if (iq) {
						tmp += (niq ? ct.substring(1) : ct) + " ";
					}else {
						tcargs[nl++] = ct;
					}
					if ((!niq || ct.length() > 3) && iq && ct.endsWith("\"")) {
						iq = false;
						String n = tmp.trim();
						if (n.endsWith("\"")) n = n.substring(0, n.length() - 1);
						tcargs[nl++] = n;
						tmp = "";
					}
				}
				args = new String[nl];
				System.arraycopy(tcargs, 0, args, 0, nl);
			}
			if (com.equals("ttl")) {
				if (args.length != 1) {
					Logger.log(f.getAbsolutePath() + ": Invalid ttl directive @ line " + ln);
					continue;
				}
				try {
					ttl = Integer.parseInt(args[0]);
				}catch (NumberFormatException e) {
					Logger.log(f.getAbsolutePath() + ": Invalid ttl directive at line " + ln);
					continue;
				}
				dirs.add(new Directive("ttl", args));
			}else if (com.equals("import")) {
				if (args.length != 1) {
					Logger.log(f.getAbsolutePath() + ": Invalid import directive at line " + ln);
					continue;
				}
				boolean isabs = false;
				if (AvunaHTTPD.windows) {
					if (args[0].length() > 0 && args[0].substring(1).startsWith(":")) {
						isabs = true;
					}
				}else {
					isabs = args[0].startsWith("/");
				}
				File nf = isabs ? new File(args[0]) : new File(f.getParentFile(), args[0]);
				if (!nf.exists()) {
					Logger.log(nf.getAbsolutePath() + ": file does not exist for import at line " + ln);
					continue;
				}
				if (!nf.canRead()) {
					Logger.log(nf.getAbsolutePath() + ": invalid permissions for import at line " + ln);
					continue;
				}
				subload(nf, dirs);
				dirs.add(new Directive("ttl", args));
			}else if (com.equals("zone")) {
				if (args.length != 2) {
					Logger.log(f.getAbsolutePath() + ": Invalid zone directive at line " + ln);
					continue;
				}
				boolean isabs = false;
				if (AvunaHTTPD.windows) {
					if (args[1].length() > 0 && args[1].substring(1).startsWith(":")) {
						isabs = true;
					}
				}else {
					isabs = args[1].startsWith("/");
				}
				File nf = isabs ? new File(args[1]) : new File(f.getParentFile(), args[1]);
				if (!nf.exists()) {
					Logger.log(nf.getAbsolutePath() + ": file does not exist for subzone at line " + ln);
					continue;
				}
				if (!nf.canRead()) {
					Logger.log(nf.getAbsolutePath() + ": invalid permissions for subzone at line " + ln);
					continue;
				}
				ZoneFile sz = new ZoneFile(nf);
				sz.load();
				dirs.add(new ZoneDirective(args, sz));
			}else {
				String[] nargs = new String[args.length + 1];
				nargs[0] = com;
				System.arraycopy(args, 0, nargs, 1, args.length);
				args = nargs;
				DNSRecord record = readRecord(ttl, args);
				if (record == null) {
					Logger.log(f.getAbsolutePath() + ": malformed record at line " + ln);
					continue;
				}
				dirs.add(record);
			}
			ln++;
		}
		s.close();
	}
	
	private static DNSRecord readRecord(int ttl, String[] pa) throws IOException {
		String[] args = pa;
		if (args.length < 3) return null;
		String domain = args[0];
		Type type = Type.getType(args[1].toUpperCase());
		String[] nargs = new String[args.length - 2];
		System.arraycopy(args, 2, nargs, 0, args.length);
		args = nargs;
		byte[] fd = null;
		if (type == Type.A) {
			fd = Util.ipToByte(args[0]);
		}else if (type == Type.AAAA) {
			fd = Util.ip6ToByte(args[0]);
		}else if (type == Type.MX) {
			if (args.length != 2) {
				Logger.log("[WARNING] MALFORMED MX! Skipping.");
				return null;
			}
			int priority = 0;
			try {
				Integer.parseInt(args[0]);
			}catch (NumberFormatException e) {
				Logger.log("[WARNING] MALFORMED MX! Skipping.");
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
		return new DNSRecord(domain, type, ttl, fd);
	}
	
	public void load() throws IOException {
		ArrayList<IDirective> dirs = new ArrayList<IDirective>();
		subload(f, dirs);
		this.dirs = dirs.toArray(new IDirective[0]);
	}
}
