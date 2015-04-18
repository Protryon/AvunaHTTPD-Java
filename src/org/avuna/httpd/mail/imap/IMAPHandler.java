package org.avuna.httpd.mail.imap;

import java.util.ArrayList;
import org.avuna.httpd.hosts.HostMail;
import org.avuna.httpd.mail.imap.command.IMAPCommandAppend;
import org.avuna.httpd.mail.imap.command.IMAPCommandAuthenticate;
import org.avuna.httpd.mail.imap.command.IMAPCommandAuthenticateCont;
import org.avuna.httpd.mail.imap.command.IMAPCommandCapability;
import org.avuna.httpd.mail.imap.command.IMAPCommandCheck;
import org.avuna.httpd.mail.imap.command.IMAPCommandClose;
import org.avuna.httpd.mail.imap.command.IMAPCommandCopy;
import org.avuna.httpd.mail.imap.command.IMAPCommandCreate;
import org.avuna.httpd.mail.imap.command.IMAPCommandDelete;
import org.avuna.httpd.mail.imap.command.IMAPCommandExamine;
import org.avuna.httpd.mail.imap.command.IMAPCommandExpunge;
import org.avuna.httpd.mail.imap.command.IMAPCommandFetch;
import org.avuna.httpd.mail.imap.command.IMAPCommandList;
import org.avuna.httpd.mail.imap.command.IMAPCommandLogin;
import org.avuna.httpd.mail.imap.command.IMAPCommandLogout;
import org.avuna.httpd.mail.imap.command.IMAPCommandLsub;
import org.avuna.httpd.mail.imap.command.IMAPCommandNoop;
import org.avuna.httpd.mail.imap.command.IMAPCommandRename;
import org.avuna.httpd.mail.imap.command.IMAPCommandSearch;
import org.avuna.httpd.mail.imap.command.IMAPCommandSelect;
import org.avuna.httpd.mail.imap.command.IMAPCommandStarttls;
import org.avuna.httpd.mail.imap.command.IMAPCommandStatus;
import org.avuna.httpd.mail.imap.command.IMAPCommandStore;
import org.avuna.httpd.mail.imap.command.IMAPCommandSubscribe;
import org.avuna.httpd.mail.imap.command.IMAPCommandUID;
import org.avuna.httpd.mail.imap.command.IMAPCommandUnsubscribe;

public class IMAPHandler {
	public final ArrayList<IMAPCommand> commands = new ArrayList<IMAPCommand>();
	
	public IMAPHandler(final HostMail host) {
		commands.add(new IMAPCommandCapability("capability", 0, 100, host));
		commands.add(new IMAPCommandLogout("logout", 0, 100, host));
		commands.add(new IMAPCommandNoop("noop", 0, 100, host));
		commands.add(new IMAPCommandStarttls("starttls", 0, 0, host));
		commands.add(new IMAPCommandLogin("login", 0, 0, host));
		commands.add(new IMAPCommandAuthenticate("authenticate", 0, 0, host));
		commands.add(new IMAPCommandAuthenticateCont("", 1, 1, host));
		commands.add(new IMAPCommandSelect("select", 2, 100, host));
		commands.add(new IMAPCommandExamine("examine", 2, 100, host));
		commands.add(new IMAPCommandCreate("create", 2, 100, host));
		commands.add(new IMAPCommandDelete("delete", 2, 100, host));
		commands.add(new IMAPCommandRename("rename", 2, 100, host));
		commands.add(new IMAPCommandSubscribe("subscribe", 2, 100, host));
		commands.add(new IMAPCommandUnsubscribe("unsubscribe", 2, 100, host));
		commands.add(new IMAPCommandList("list", 2, 100, host));
		commands.add(new IMAPCommandLsub("lsub", 2, 100, host));
		commands.add(new IMAPCommandStatus("status", 2, 100, host));
		commands.add(new IMAPCommandAppend("append", 2, 100, host));
		commands.add(new IMAPCommandCheck("check", 3, 100, host));
		commands.add(new IMAPCommandClose("close", 3, 100, host));
		commands.add(new IMAPCommandExpunge("expunge", 3, 100, host));
		final IMAPCommandSearch search;
		commands.add(search = new IMAPCommandSearch("search", 3, 100, host));
		final IMAPCommandFetch fetch;
		commands.add(fetch = new IMAPCommandFetch("fetch", 3, 100, host));
		final IMAPCommandStore store;
		commands.add(store = new IMAPCommandStore("store", 3, 100, host));
		commands.add(new IMAPCommandCopy("copy", 3, 100, host));
		commands.add(new IMAPCommandUID("uid", 3, 100, host, fetch, store, search));
	}
}
