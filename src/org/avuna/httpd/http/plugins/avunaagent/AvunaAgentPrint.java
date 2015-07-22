/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.http.plugins.avunaagent;

import org.avuna.httpd.http.networking.RequestPacket;
import org.avuna.httpd.http.networking.ResponsePacket;

public abstract class AvunaAgentPrint extends AvunaAgent {
	
	/** Generate a webpage. If you throw a RuntimeException of any kind, a 500 Internal Server Error will be returned. Look into the argument classes for more information on the API.
	 * 
	 * @param out Used to build HTML to output.
	 * @param response A ResponsePacket that is being prepared for the client. Write changes here.
	 * @param request The (almost) original packet sent by the Client.
	 * @return A boolean value, if false, a blank page will be sent, usually for invalid(ie should-never-happen) requests */
	public abstract boolean generate(HTMLBuilder out, ResponsePacket response, RequestPacket request);
	
	public final int getType() {
		return 1;
	}
	
}
