package org.avuna.httpd.dns.zone;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.dns.Type;
import org.avuna.httpd.util.Logger;

public class ZoneFile {
	private File f = null;
	private List<IDirective> dirs = Collections.synchronizedList(new ArrayList<IDirective>());
	
	public List<IDirective> getDirectives() {
		return dirs;
	}
	
	public ZoneFile(File f) {
		this.f = f;
	}
	
	public ZoneFile() {
		this.f = null;
	}
	
	public static String[] congealArgsEscape(String[] args) {
		String[] tcargs = new String[args.length];
		int nl = 0;
		boolean iq = false;
		String tmp = "";
		for (int i = 0; i < args.length; i++) {
			boolean niq = false;
			String ct = args[i].trim();
			if (!iq && ct.startsWith("\"") && (ct.length() < 2 || ct.charAt(ct.length() - 2) != '\\')) {
				iq = true;
				niq = true;
			}
			if (iq) {
				tmp += (niq ? ct.substring(1) : ct) + " ";
			}else {
				tcargs[nl++] = ct;
			}
			if ((!niq || ct.length() > 3) && iq && ct.endsWith("\"") && (ct.length() < 2 || ct.charAt(ct.length() - 2) != '\\')) {
				iq = false;
				String n = tmp.trim();
				if (n.endsWith("\"")) n = n.substring(0, n.length() - 1);
				tcargs[nl++] = n;
				tmp = "";
			}
		}
		String[] ret = new String[nl];
		System.arraycopy(tcargs, 0, ret, 0, nl);
		for (int i = 0; i < ret.length; i++) {
			ret[i] = ret[i].replace("\\\"", "\"").replace("\\\\", "\\");
		}
		return ret;
	}
	
	private static void subload(File f, ArrayList<IDirective> dirs) throws IOException {
		int ttl = 3600;
		int ln = 0;
		Scanner s = new Scanner(f);
		while (s.hasNextLine()) {
			ln++;
			String line = s.nextLine().trim();
			String com = line.substring(0, line.contains(" ") ? line.indexOf(" ") : line.length());
			line = line.substring(com.length()).trim();
			String[] args = line.split(" ");
			if (args.length > 0) {
				args = congealArgsEscape(args);
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
					if (args[0].length() > 0 && args[0].substring(0).startsWith(":")) {
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
				ZoneFile sz = new ZoneFile(nf);
				sz.load();
				dirs.add(new ImportDirective(args, sz));
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
		}
		s.close();
	}
	
	private static DNSRecord readRecord(int ttl, String[] pa) throws IOException {
		String[] args = pa;
		if (args.length < 3) return null;
		String domain = args[0];
		Type type = Type.getType(args[1].toUpperCase());
		String[] nargs = new String[args.length - 2];
		System.arraycopy(args, 2, nargs, 0, args.length - 2);
		args = nargs;
		byte[] fd = DNSRecord.getDataFromArgs(type, args);
		return new DNSRecord(domain, type, ttl, fd, pa);
	}
	
	public void load() throws IOException {
		ArrayList<IDirective> dirs = new ArrayList<IDirective>();
		subload(f, dirs);
		this.dirs.clear();
		this.dirs.addAll(dirs);
	}
	
	public static String escape(String s) {
		String f = s;
		f = f.replace("\\", "\\\\").replace("\"", "\\\"");
		if (f.contains(" ")) f = "\"" + f + "\"";
		return f;
	}
	
	public void save() throws IOException {
		PrintStream ps = new PrintStream(f);
		for (IDirective dir : dirs) {
			if (dir instanceof DNSRecord) {
				DNSRecord rec = (DNSRecord)dir;
				String l = escape(rec.getDomain()) + " " + escape(rec.getType().name);
				for (String ss : rec.getTV()) {
					l += " " + escape(ss);
				}
				ps.println(l);
			}else if (dir instanceof Directive) {
				Directive idir = (Directive)dir;
				String l = idir.name;
				for (String s : idir.args) {
					l += " " + escape(s);
				}
				ps.println(l);
				if (dir instanceof ImportDirective) {
					((ImportDirective)dir).zf.save();
				}else if (dir instanceof ZoneDirective) {
					((ZoneDirective)dir).zf.save();
				}
			}
		}
		ps.flush();
		ps.close();
	}
	
	public File getFile() {
		return f;
	}
}
