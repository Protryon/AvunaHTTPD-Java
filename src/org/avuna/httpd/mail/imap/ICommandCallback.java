package org.avuna.httpd.mail.imap;

import java.io.IOException;

public interface ICommandCallback {
	public void receiveBlock(IMAPWork focus, byte[] block, IMAPBlockStatus status) throws IOException;
}
