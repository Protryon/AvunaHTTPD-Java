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
