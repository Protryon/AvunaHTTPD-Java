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
