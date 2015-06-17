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

package org.avuna.httpd.http.util;

import org.avuna.httpd.util.Logger;

public class CompiledDirective {
	public final Directive directive;
	public final String[] args;
	
	public CompiledDirective(Directive directive, String args) {
		this.directive = directive;
		String[] cargs = args.length() > 0 ? args.split(" ") : new String[0];
		String[] tcargs = new String[cargs.length];
		int nl = 0;
		boolean iq = false;
		String tmp = "";
		for (int i = 0; i < cargs.length; i++) {
			boolean niq = false;
			String ct = cargs[i].trim();
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
		cargs = new String[nl];
		System.arraycopy(tcargs, 0, cargs, 0, nl);
		switch (directive) {
		case forbid:
			if (cargs.length != 1) {
				Logger.log("[WARNING] FORBID directive has invalid arguments: " + args + " expecting one argument.");
				throw new IllegalArgumentException();
			}
			break;
		case redirect:
			if (cargs.length != 2) {
				Logger.log("[WARNING] REDIRECT directive has invalid arguments: " + args + " expecting two arguments.");
				throw new IllegalArgumentException();
			}
			break;
		case index:
			if (cargs.length == 0) {
				Logger.log("[WARNING] INDEX directive has invalid arguments: " + args + " expecting at least one argument.");
				throw new IllegalArgumentException();
			}
			break;
		case mime:
			if (cargs.length != 2) {
				Logger.log("[WARNING] MIME directive has invalid arguments: " + args + " expecting one mime-type argument, and a regex argument.");
				throw new IllegalArgumentException();
			}
			break;
		case cache:
			if (cargs.length != 2) {
				Logger.log("[WARNING] CACHE directive has invalid arguments: " + args + " expecting a cache argument(off, or seconds, or -1 for permanent), and a regex argument.");
				throw new IllegalArgumentException();
			}
			break;
		case rewrite:
			if (cargs.length != 2) {
				Logger.log("[WARNING] CACHE directive has invalid arguments: " + args + " expecting two arguments.");
				throw new IllegalArgumentException();
			}
			break;
		}
		this.args = cargs;
	}
}
