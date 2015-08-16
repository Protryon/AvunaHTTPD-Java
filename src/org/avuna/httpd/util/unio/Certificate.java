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
