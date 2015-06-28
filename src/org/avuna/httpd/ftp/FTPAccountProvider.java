/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.ftp;

import java.io.File;

public abstract class FTPAccountProvider {
	public abstract boolean isValid(String user, String pass);
	
	public abstract File getRoot(String user);
	
	public abstract int getUID(String user);
	
	public abstract int getGID(String user);
	
	public abstract String getExternalIP();
}
