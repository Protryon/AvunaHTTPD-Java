package org.avuna.httpd.http.plugins.ssi.functions;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.http.plugins.ssi.Page;
import org.avuna.httpd.http.plugins.ssi.SSIFunction;

public class MD5Function extends SSIFunction {
	
	@Override
	public String call(Page page, String arg) {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("MD5");
			sha1.update(arg.getBytes());
			return AvunaHTTPD.fileManager.bytesToHex(sha1.digest());
		}catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
}
