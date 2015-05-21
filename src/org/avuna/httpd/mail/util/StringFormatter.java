package org.avuna.httpd.mail.util;

public class StringFormatter {
	public static String[] congealBySurroundings(String[] orig, String s1, String s2) {
		String[] nargs = new String[orig.length];
		String ctps = "";
		int cloc = 0;
		int clen = 0;
		boolean act = false;
		int nlen = orig.length;
		for (int i = 0; i < orig.length; i++) {
			if (!act && orig[i].contains(s1)) {
				act = true;
				ctps = "";
				cloc = i;
				nlen += 1;
			}
			if (act) {
				ctps += orig[i] + " ";
				clen++;
				nlen--;
				if (orig[i].contains(s2)) {
					ctps = ctps.trim();
					nargs[cloc] = ctps;
					act = false;
				}
			}else {
				nargs[i] = orig[i];
			}
		}
		String[] n = new String[nlen];
		for (int i = 0; i < nlen; i++) {
			n[i] = nargs[i];
		}
		return n;
	}
}
