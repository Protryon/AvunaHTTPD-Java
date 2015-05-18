package org.avuna.httpd.com;

import java.io.PrintStream;
import java.util.Scanner;

public class CommandContext {
	private String selectedHost = "main", selectedVHost = "main";
	private PrintStream out = null;
	private Scanner in = null;
	private int[] status = new int[0];
	private int[] cmdRan = new int[0];
	private final CommandRegistry registry;
	
	public final CommandRegistry getRegistry() {
		return registry;
	}
	
	// TODO: finish command logs
	protected synchronized final void logCommand(int cmd, int res) {
		int[] ns = new int[cmdRan.length + 1];
		System.arraycopy(cmdRan, 0, ns, 0, cmdRan.length);
		ns[cmdRan.length] = cmd;
		cmdRan = ns;
		ns = new int[status.length + 1];
		System.arraycopy(status, 0, ns, 0, status.length);
		ns[status.length] = res;
		status = ns;
	}
	
	public void println(String line) {
		if (out != null) out.println(line);
	}
	
	protected CommandContext(CommandRegistry registry, PrintStream out, Scanner in) {
		this.out = out;
		this.in = in;
		this.registry = registry;
	}
	
	public String getSelectedHost() {
		return selectedHost;
	}
	
	public void setSelectedHost(String selectedHost) {
		this.selectedHost = selectedHost;
	}
	
	public String getSelectedVHost() {
		return selectedVHost;
	}
	
	public void setSelectedVHost(String selectedVHost) {
		this.selectedVHost = selectedVHost;
	}
	
	public PrintStream getOut() {
		return out;
	}
	
	public Scanner getIn() {
		return in;
	}
}
