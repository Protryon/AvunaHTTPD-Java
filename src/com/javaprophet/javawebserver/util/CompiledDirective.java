package com.javaprophet.javawebserver.util;

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
				System.out.println("[WARNING] FORBID directive has invalid arguments: " + args + " expecting one argument.");
				throw new IllegalArgumentException();
			}
			break;
		case redirect:
			if (cargs.length != 2) {
				System.out.println("[WARNING] REDIRECT directive has invalid arguments: " + args + " expecting two argument.");
				throw new IllegalArgumentException();
			}
			break;
		case index:
			if (cargs.length == 0) {
				System.out.println("[WARNING] INDEX directive has invalid arguments: " + args + " expecting at least one argument.");
				throw new IllegalArgumentException();
			}
			break;
		case mime:
			if (cargs.length < 2) {
				System.out.println("[WARNING] MIME directive has invalid arguments: " + args + " expecting at one mime-type argument, and at least one extension/filename(ex .txt or index.txt).");
				throw new IllegalArgumentException();
			}
			break;
		}
		this.args = cargs;
	}
}
