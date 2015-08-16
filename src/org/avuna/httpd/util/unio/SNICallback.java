package org.avuna.httpd.util.unio;

public abstract class SNICallback {
	public abstract Certificate getCertificate(String domain);
	
	public final long getSNICert(String domain) {
		Certificate cert = getCertificate(domain);
		return cert == null ? 0L : cert.getRawCertificate();
	}
}
