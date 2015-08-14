/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.mail.imap;

import java.io.IOException;
import org.avuna.httpd.hosts.HostMail;

public abstract class IMAPCommand {
	public final String comm;
	public final int minState, maxState;
	protected final HostMail host;
	
	public IMAPCommand(String comm, int minState, int maxState, HostMail host) {
		this.host = host;
		this.comm = comm;
		this.minState = minState;
		this.maxState = maxState;
	}
	
	public abstract void run(IMAPWork focus, String letters, String[] args) throws IOException;
}
