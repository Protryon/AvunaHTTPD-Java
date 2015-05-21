package org.avuna.httpd.com.base;

import org.avuna.httpd.com.Command;
import org.avuna.httpd.com.CommandContext;

public class CommandSelect extends Command {
	
	@Override
	public int processCommand(String[] args, CommandContext context) throws Exception {
		if (args.length != 1 && args.length != 2) {
			context.println("Usage: select <Host> <VHost if applicable>");
			return 1;
		}
		context.setSelectedHost(args[0]);
		if (args.length == 2) context.setSelectedVHost(args[1]);
		context.println("Selected " + args[0] + (args.length == 2 ? ("/" + args[1]) : "") + "!");
		return 0;
	}
	
	@Override
	public String getHelp() {
		return "Select a Host and/or VHost for use in other commands.";
	}
	
}
