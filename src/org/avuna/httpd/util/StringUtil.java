package org.avuna.httpd.util;

public abstract class StringUtil {
	public static String toLowerCase(String input) {
		char[] inc = input.toCharArray();
		if (toLowerCase(inc)) {
			return new String(inc);
		}else {
			return input;
		}
	}
	
	public static String escape(String original, char[] spec) {
		String n = original.replace("\\", "\\\\");
		for (char c : spec) {
			n = n.replace(c + "", "\\" + c);
		}
		return n;
	}
	
	public static String unescape(String original, char[] spec) {
		String n = original;
		for (char c : spec) {
			n = n.replace("\\" + c, "" + c);
		}
		n = original.replace("\\\\", "\\");
		
		return n;
	}
	
	public static boolean toLowerCase(char[] input) {
		boolean ch = false;
		for (int i = 0; i < input.length; i++) {
			char c = input[i];
			if (c >= 65 && c <= 90) {
				input[i] = (char) (c + 32);
				ch = true;
			}
		}
		return ch;
	}
	
	public static boolean containsAny(String input, String... matching) {
		char[][] mcs = new char[matching.length][];
		for (int i = 0; i < matching.length; i++) {
			mcs[i] = matching[i].toCharArray();
		}
		return containsAny(input.toCharArray(), mcs);
	}
	
	public static boolean containsAny(char[] input, char[]... matching) {
		int[] ml = new int[matching.length];
		for (int i = 0; i < input.length; i++) {
			char inc = input[i];
			for (int o = 0; o < ml.length; o++) {
				if (matching[o][ml[o]] == inc) {
					ml[o]++;
				}else if (ml[o] > 0) {
					ml[o] = 0;
				}
				if (ml[o] == matching[o].length) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static char[] replace(char[] input, char[] rf, char[] rt) {
		int isl = input.length;
		int rfsl = rf.length;
		int rtsl = rt.length;
		int ml = 0;
		int mc = 0;
		for (int i = 0; i < isl; i++) {
			boolean im = input[i] == rf[ml];
			if (im) {
				ml++;
				if (ml == rfsl) {
					mc++;
					ml = 0;
					continue;
				}
			}else if (ml > 0) {
				ml = 0;
			}
		}
		ml = 0;
		char[] output = new char[isl - ((rfsl - rtsl) * mc)];
		int mi = 0;
		for (int i = 0; i < isl; i++) {
			boolean im = input[i] == rf[ml];
			if (im) {
				ml++;
				if (ml == rfsl) {
					int os = mi - ml + 1;
					System.arraycopy(rt, 0, output, os, rt.length);
					if (ml > rtsl) {
						int ofs = ml - rtsl;
						int oi = os + ofs;
						if (oi + ofs < output.length) System.arraycopy(new char[ofs], 0, output, oi, ofs);
					}
					mi += ml - rtsl - 1;
					ml = 0;
					continue;
				}
			}else if (ml > 0) {
				ml = 0;
			}
			output[mi++] = input[i];
		}
		return output;
	}
	
	public static String replace(String input, String rf, String rt) {
		return new String(replace(input.toCharArray(), rf.toCharArray(), rt.toCharArray()));
	}
}
