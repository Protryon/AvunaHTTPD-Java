package org.avuna.httpd.com.base;

import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.com.Command;
import org.avuna.httpd.com.CommandContext;
import org.avuna.httpd.hosts.Host;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.mailbox.EmailAccount;

public class CommandRegister extends Command {
	
	@Override
	public int processCommand(String[] args, CommandContext context) throws Exception {
		Host host = AvunaHTTPD.hosts.get(context.getSelectedHost());
		if (!(host instanceof HostMail)) {
			context.println("Not a mail host!");
			return 4;
		}
		if (args.length != 2) {
			context.println("Usage: register <email> <password>");
			return 1;
		}
		((HostMail)host).accounts.add(new EmailAccount(args[0], args[1]));
		context.println("Registered " + args[0] + "!");
		return 0;
	}
	
	@Override
	public String getHelp() {
		return "Forcefully registers a mail account on the selected mail server.";
	}
}
