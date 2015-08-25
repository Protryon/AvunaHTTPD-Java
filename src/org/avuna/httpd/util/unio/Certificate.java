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
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.avuna.httpd.util.unio;

import org.avuna.httpd.util.CException;

public class Certificate {
	private final long cert;
	
	public Certificate(String ca, String cert, String key) throws CException {
		this.cert = GNUTLS.loadcert(ca, cert, key);
		if (this.cert <= 0L) {
			throw new CException((int) this.cert, "Failed to load SSL certificate!");
		}
	}
	
	public Certificate(long rawCert) {
		this.cert = rawCert;
	}
	
	public final long getRawCertificate() {
		return cert;
	}
}
