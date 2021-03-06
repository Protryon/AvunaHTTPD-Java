/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

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
import org.avuna.httpd.hosts.HostDNS;
import org.avuna.httpd.mail.util.StringFormatter;

public class ZoneFile {
	private File f = null;
	private List<IDirective> dirs = Collections.synchronizedList(new ArrayList<IDirective>());
	
	public List<IDirective> getDirectives() {
		return getDirectives(false);
	}
	
	public List<IDirective> getDirectives(boolean round) {
		if (!round) return dirs;
		ArrayList<IDirective> ndirs = new ArrayList<IDirective>();
		boolean rnd = false;
		int rl = -1;
		ArrayList<IDirective> rb = new ArrayList<IDirective>();
		for (IDirective id : dirs) {
			if (!rnd && id instanceof RoundStartDirective) {
				rnd = true;
				rl = ((RoundStartDirective) id).limit;
				ndirs.add(id);
			}else if (rnd && id instanceof RoundStopDirective) {
				rnd = false;
				Collections.shuffle(rb);
				if (rl < 1) {
					ndirs.addAll(rb);
				}else for (int i = 0; i < rl; i++) {
					if (i < rb.size()) ndirs.add(rb.get(i));
				}
				rb.clear();
				rl = -1;
				ndirs.add(id);
			}else {
				if (rnd) {
					rb.add(id);
				}else {
					ndirs.add(id);
				}
			}
		}
		return ndirs;
	}
	
	public ZoneFile(File f) {
		this.f = f;
	}
	
	public ZoneFile() {
		this.f = null;
	}
	
	private static void subload(HostDNS host, File f, ArrayList<IDirective> dirs) throws IOException {
		int ln = 0;
		Scanner s = new Scanner(f);
		boolean round = false;
		while (s.hasNextLine()) {
			ln++;
			String line = s.nextLine();
			if (line.contains("#")) line = line.substring(0, line.indexOf("#")).trim();
			if (line.length() == 0) continue;
			String com = line.substring(0, line.contains(" ") ? line.indexOf(" ") : line.length());
			line = line.substring(com.length()).trim();
			String[] args = line.split(" ");
			if (args.length > 0) {
				args = StringFormatter.congealArgsEscape(args);
			}
			if (com.equals("import")) {
				if (args.length != 1) {
					host.logger.log(f.getAbsolutePath() + ": Invalid import directive at line " + ln);
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
					host.logger.log(nf.getAbsolutePath() + ": file does not exist for import at line " + ln);
					continue;
				}
				if (!nf.canRead()) {
					host.logger.log(nf.getAbsolutePath() + ": invalid permissions for import at line " + ln);
					continue;
				}
				ZoneFile sz = new ZoneFile(nf);
				sz.load(host);
				dirs.add(new ImportDirective(args, sz));
			}else if (com.equals("zone")) {
				if (args.length != 2) {
					host.logger.log(f.getAbsolutePath() + ": Invalid zone directive at line " + ln);
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
					host.logger.log(nf.getAbsolutePath() + ": file does not exist for subzone at line " + ln);
					continue;
				}
				if (!nf.canRead()) {
					host.logger.log(nf.getAbsolutePath() + ": invalid permissions for subzone at line " + ln);
					continue;
				}
				ZoneFile sz = new ZoneFile(nf);
				sz.load(host);
				dirs.add(new ZoneDirective(args, sz));
			}else if (com.equals("roundstart")) {
				if (round) {
					host.logger.log(f.getAbsolutePath() + ": roundstart called, when already in round robin at line " + ln + ", ignored.");
					continue;
				}
				round = true;
				dirs.add(new RoundStartDirective(args));
			}else if (com.equals("roundstop")) {
				if (!round) {
					host.logger.log(f.getAbsolutePath() + ": roundstop called, when not in round robin at line " + ln + ", ignored.");
					continue;
				}
				round = false;
				dirs.add(new RoundStopDirective(args));
			}else {
				String[] nargs = new String[args.length + 1];
				nargs[0] = com;
				System.arraycopy(args, 0, nargs, 1, args.length);
				args = nargs;
				DNSRecord record = readRecord(args);
				if (record == null) {
					host.logger.log(f.getAbsolutePath() + ": malformed record at line " + ln);
					continue;
				}
				dirs.add(record);
			}
		}
		s.close();
	}
	
	private static DNSRecord readRecord(String[] pa) throws IOException {
		String[] args = pa;
		if (args.length < 4) return null;
		String domain = args[0];
		Type type = Type.getType(args[1].toUpperCase());
		String ttl = args[2];
		int ttlr1 = 3600, ttlr2 = 3600;
		if (ttl.contains("-")) {
			ttlr1 = Integer.parseInt(ttl.substring(0, ttl.indexOf("-")));
			ttlr2 = Integer.parseInt(ttl.substring(ttl.indexOf("-") + 1));
			if (ttlr1 > ttlr2) {
				int temp = ttlr2;
				ttlr2 = ttlr1;
				ttlr1 = temp;
			}
		}else {
			ttlr1 = Integer.parseInt(ttl);
			ttlr2 = ttlr1;
		}
		String[] nargs = new String[args.length - 3];
		System.arraycopy(args, 3, nargs, 0, nargs.length);
		args = nargs;
		byte[] fd = DNSRecord.getDataFromArgs(type, args);
		if (fd == null) return null;
		return new DNSRecord(domain, type, ttlr1, ttlr2, fd, pa, args);
	}
	
	public void load(HostDNS host) throws IOException {
		ArrayList<IDirective> dirs = new ArrayList<IDirective>();
		subload(host, f, dirs);
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
				DNSRecord rec = (DNSRecord) dir;
				String l = escape(rec.getDomain()) + " " + escape(rec.getType().name) + " " + rec.getTimeToLive();
				for (String ss : rec.getTV()) {
					l += " " + escape(ss);
				}
				ps.println(l);
			}else if (dir instanceof Directive) {
				Directive idir = (Directive) dir;
				String l = idir.name;
				for (String s : idir.args) {
					l += " " + escape(s);
				}
				ps.println(l);
				if (dir instanceof ImportDirective) {
					((ImportDirective) dir).zf.save();
				}else if (dir instanceof ZoneDirective) {
					((ZoneDirective) dir).zf.save();
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
