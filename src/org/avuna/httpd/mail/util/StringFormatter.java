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

package org.avuna.httpd.mail.util;

public class StringFormatter {
	public static String[] congealBySurroundings(String[] orig, String s1, String s2) {
		String[] nargs = new String[orig.length + 16];
		String ctps = "";
		boolean act = false;
		int nlen = 0;
		for (int i = 0; i < orig.length; i++) {
			if (!act && orig[i].contains(s1)) {
				act = true;
				ctps = "";
			}
			if (act) {
				ctps += orig[i] + " ";
				if (orig[i].contains(s2)) {
					ctps = ctps.trim();
					nargs[nlen++] = ctps;
					act = false;
				}
			}else {
				nargs[nlen++] = orig[i];
			}
		}
		String[] n = new String[nlen];
		for (int i = 0; i < nlen; i++) {
			n[i] = nargs[i];
		}
		return n;
	}
}
