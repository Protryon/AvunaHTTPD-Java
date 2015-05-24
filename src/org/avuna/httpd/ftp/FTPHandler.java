package org.avuna.httpd.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Random;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.hosts.HostFTP;
import org.avuna.httpd.util.Logger;

public class FTPHandler {
	
	public final ArrayList<FTPCommand> commands = new ArrayList<FTPCommand>();
	private final Random rand = new Random();
	private static final SimpleDateFormat mdtm = new SimpleDateFormat("yyyyMMddHHmmss");
	
	public FTPHandler(final HostFTP host) {
		commands.add(new FTPCommand("user", 0, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				focus.writeLine(331, "Please specify the password.");
				focus.user = line;
			}
		});
		commands.add(new FTPCommand("acct", 0, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				focus.writeLine(502, "ACCT not implemented.");
			}
		});
		commands.add(new FTPCommand("pass", 0, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				if (focus.user == null || focus.user.length() == 0) {
					focus.writeLine(503, "Login with USER first.");
					return;
				}
				if (host.provider.isValid(focus.user, line)) {
					focus.writeLine(230, "Login successful.");
					focus.auth = true;
					focus.state = 1;
					focus.root = host.provider.getRoot(focus.user);
				}else {
					focus.writeLine(531, "Login incorrect.");
				}
			}
		});
		commands.add(new FTPCommand("quit", 0, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				focus.writeLine(221, "Terminating connection.");
				focus.s.close();
			}
		});
		commands.add(new FTPCommand("cwd", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				focus.cwd = line;
				focus.writeLine(250, "Directory successfully changed.");
			}
		});
		commands.add(new FTPCommand("cdup", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				focus.cwd = line;
				focus.writeLine(250, "Directory successfully changed.");
			}
		});
		commands.add(new FTPCommand("smnt", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				focus.writeLine(502, "SMNT not implemented.");
			}
		});
		commands.add(new FTPCommand("rein", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				focus.writeLine(502, "REIN not implemented.");
			}
		});
		commands.add(new FTPCommand("port", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				focus.writeLine(500, "Illegal PORT command.");// TODO
			}
		});
		commands.add(new FTPCommand("pasv", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				if (focus.isPASV) {
					focus.writeLine(425, "Already in PASV mode!");
					return;
				}else if (focus.isPORT) {
					focus.writeLine(425, "Already in PORT mode!");
					return;
				}
				focus.isPASV = true;
				ServerSocket ps = null;
				do {
					try {
						int port = rand.nextInt(65534 - 1024) + 1024;
						ps = new ServerSocket(port);
						int minor = (port % 256);
						int major = (port - minor) / 256;
						focus.psv = new ThreadPassive(focus, ps);
						focus.psv.start();
						focus.writeLine(227, "Entering Passive Mode (127,0,0,1," + major + "," + minor + ").");
					}catch (IOException e) {
						Logger.logError(e);
						ps = null;
					}
				}while (ps == null);
			}
		});
		commands.add(new FTPCommand("type", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				if (line.equalsIgnoreCase("a")) {
					focus.type = FTPType.ASCII;
					focus.writeLine(200, "Switching to ASCII mode.");
				}else if (line.equalsIgnoreCase("i") || line.toLowerCase().startsWith("l")) {
					focus.type = FTPType.BINARY;
					focus.writeLine(200, "Switching to Binary mode.");
				}else {
					focus.writeLine(500, "Unrecognised TYPE command.");
				}
			}
		});
		commands.add(new FTPCommand("stru", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				if (line.equalsIgnoreCase("f")) {
					focus.writeLine(200, "Structure set to F.");
				}else {
					focus.writeLine(504, "Bad STRU command.");
				}
			}
		});
		commands.add(new FTPCommand("mode", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				if (line.equalsIgnoreCase("s")) {
					focus.writeLine(200, "Mode set to S.");
				}else {
					focus.writeLine(504, "Bad MODE command.");
				}
			}
		});
		commands.add(new FTPCommand("stor", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				if (!focus.isPASV && !focus.isPORT) {
					focus.writeLine(425, "Use PORT or PASV first.");
					return;
				}
				File rt = isAbsolute(line) ? new File(focus.root) : new File(focus.root, focus.cwd);
				File f = new File(rt, line);
				if (!f.getAbsolutePath().startsWith(focus.root)) {
					focus.writeLine(550, "Failed to open file.");
					return;
				}
				boolean s = false;
				try {
					if (f.exists()) f.delete();
					f.createNewFile();
					s = true;
				}catch (IOException e) {
					// Logger.logError(e);
				}
				if (s && f.exists() && f.canWrite()) {
					focus.psv.setType(FTPTransferType.STOR, f);
				}else {
					focus.writeLine(553, "Could not create file.");
				}
			}
		});
		commands.add(new FTPCommand("stou", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				if (!focus.isPASV && !focus.isPORT) {
					focus.writeLine(425, "Use PORT or PASV first.");
					return;
				}
				File rt = isAbsolute(line) ? new File(focus.root) : new File(focus.root, focus.cwd);
				File f = new File(rt, line);
				if (!f.getAbsolutePath().startsWith(focus.root)) {
					focus.writeLine(550, "Failed to open file.");
					return;
				}
				boolean s = false;
				try {
					int i = 1;
					while (f.exists()) {
						f = new File(f.getParentFile(), f.getName() + "." + (i++));
					}
					f.createNewFile();
					s = true;
				}catch (IOException e) {
					// Logger.logError(e);
				}
				if (s && f.exists() && f.canWrite()) {
					focus.psv.setType(FTPTransferType.STOU, f);
				}else {
					focus.writeLine(553, "Could not create file.");
				}
			}
		});
		commands.add(new FTPCommand("appe", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				if (!focus.isPASV && !focus.isPORT) {
					focus.writeLine(425, "Use PORT or PASV first.");
					return;
				}
				File rt = isAbsolute(line) ? new File(focus.root) : new File(focus.root, focus.cwd);
				File f = new File(rt, line);
				if (!f.getAbsolutePath().startsWith(focus.root)) {
					focus.writeLine(550, "Failed to open file.");
					return;
				}
				try {
					if (!f.exists()) f.createNewFile();
				}catch (IOException e) {
					// Logger.logError(e);
				}
				if (f.exists() && f.canWrite()) {
					focus.psv.setType(FTPTransferType.APPE, f);
				}else {
					focus.writeLine(553, "Could not create file.");
				}
			}
		});
		commands.add(new FTPCommand("list", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				if (!focus.isPASV && !focus.isPORT) {
					focus.writeLine(425, "Use PORT or PASV first.");
					return;
				}
				File rt = isAbsolute(line) ? new File(focus.root) : new File(focus.root, focus.cwd);
				File f = new File(rt, line);
				if (!f.getAbsolutePath().startsWith(focus.root)) {
					focus.writeLine(550, "Failed to open file.");
					return;
				}
				if (f.isFile()) f = f.getParentFile();
				if (f.exists() && f.canRead()) {
					focus.psv.setType(FTPTransferType.LIST, f);
				}else {
					focus.writeLine(550, "Failed to open file.");
				}
			}
		});
		commands.add(new FTPCommand("nlst", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				if (!focus.isPASV && !focus.isPORT) {
					focus.writeLine(425, "Use PORT or PASV first.");
					return;
				}
				File rt = isAbsolute(line) ? new File(focus.root) : new File(focus.root, focus.cwd);
				File f = new File(rt, line);
				if (!f.getAbsolutePath().startsWith(focus.root)) {
					focus.writeLine(550, "Failed to open file.");
					return;
				}
				if (f.isFile()) f = f.getParentFile();
				if (f.exists() && f.canRead()) {
					focus.psv.setType(FTPTransferType.NLST, f);
				}else {
					focus.writeLine(550, "Failed to open file.");
				}
			}
		});
		commands.add(new FTPCommand("retr", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				if (!focus.isPASV && !focus.isPORT) {
					focus.writeLine(425, "Use PORT or PASV first.");
					return;
				}
				File rt = isAbsolute(line) ? new File(focus.root) : new File(focus.root, focus.cwd);
				File f = new File(rt, line);
				if (!f.getAbsolutePath().startsWith(focus.root)) {
					focus.writeLine(550, "Failed to open file.");
					return;
				}
				if (f.isDirectory()) {
					focus.writeLine(550, "Failed to open file.");
					return;
				}
				if (f.exists() && f.canRead()) {
					focus.psv.setType(FTPTransferType.RETR, f);
				}else {
					focus.writeLine(550, "Failed to open file.");
				}
			}
		});
		commands.add(new FTPCommand("dele", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				File rt = isAbsolute(line) ? new File(focus.root) : new File(focus.root, focus.cwd);
				File f = new File(rt, line);
				if (!f.getAbsolutePath().startsWith(focus.root)) {
					focus.writeLine(550, "Delete operation failed.");
					return;
				}
				if (f.isDirectory()) {
					focus.writeLine(550, "Delete operation failed.");
					return;
				}
				if (f.exists() && f.canWrite()) {
					f.delete();
					focus.writeLine(250, "Delete operation successful.");
				}else {
					focus.writeLine(550, "Delete operation failed.");
				}
			}
		});
		commands.add(new FTPCommand("rnfr", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				File rt = isAbsolute(line) ? new File(focus.root) : new File(focus.root, focus.cwd);
				File f = new File(rt, line);
				if (!f.getAbsolutePath().startsWith(focus.root)) {
					focus.writeLine(550, "Rename failed.");
					return;
				}
				if (f.exists() && f.canWrite()) {
					focus.rnfr = f.getAbsolutePath();
					focus.writeLine(350, "Ready for RNTO.");
				}else {
					focus.writeLine(550, "Rename failed.");
				}
			}
		});
		commands.add(new FTPCommand("rnto", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				if (focus.rnfr.length() == 0) {
					focus.writeLine(503, "RNFR required first.");
					return;
				}
				File rt = isAbsolute(line) ? new File(focus.root) : new File(focus.root, focus.cwd);
				File f = new File(rt, line);
				if (!f.getAbsolutePath().startsWith(focus.root)) {
					focus.writeLine(550, "Rename failed.");
					return;
				}
				if (f.exists() && f.canWrite()) {
					try {
						FileInputStream in = new FileInputStream(focus.rnfr);
						FileOutputStream out = new FileOutputStream(f);
						byte[] buf = new byte[1024];
						int i = 1;
						while (i > 0) {
							i = in.read(buf);
							if (i > 0) {
								out.write(buf, 0, i);
							}
						}
						in.close();
						out.flush();
						out.close();
						focus.writeLine(250, "Rename successful.");
					}catch (IOException e) {
						focus.writeLine(550, "Rename failed.");
						
					}
				}else {
					focus.writeLine(550, "Rename failed.");
				}
			}
		});
		commands.add(new FTPCommand("noop", 0, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				focus.writeLine(200, "noop");
			}
		});
		commands.add(new FTPCommand("allo", 0, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				focus.writeLine(202, "ALLO command ignored.");
			}
		});
		commands.add(new FTPCommand("abor", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				focus.writeLine(225, "No transfer to ABOR.");// TODO
			}
		});
		commands.add(new FTPCommand("rest", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				int i = line.length() > 0 ? Integer.parseInt(line) : 0;
				focus.skip = i;
				focus.writeLine(350, "Restart position accepted (" + i + ").");// TODO
			}
		});
		commands.add(new FTPCommand("mdtm", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				File rt = isAbsolute(line) ? new File(focus.root) : new File(focus.root, focus.cwd);
				File f = new File(rt, line);
				if (!f.getAbsolutePath().startsWith(focus.root)) {
					focus.writeLine(550, "Could not get file modification time.");
					return;
				}
				if (f.exists() && f.canRead()) {
					focus.writeLine(213, mdtm.format(f.lastModified()));
				}else {
					focus.writeLine(550, "Could not get file modification time.");
				}
			}
		});
		commands.add(new FTPCommand("mkd", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				File rt = isAbsolute(line) ? new File(focus.root) : new File(focus.root, focus.cwd);
				File f = new File(rt, line);
				if (!f.getAbsolutePath().startsWith(focus.root)) {
					focus.writeLine(550, "Create directory operation failed.");
					return;
				}
				if (f.isFile()) f = f.getParentFile();
				if (f.getParentFile() != null && f.getParentFile().exists() && f.getParentFile().canWrite()) {
					f.mkdirs();
					focus.writeLine(250, "Create directory operation successful.");
				}else {
					focus.writeLine(550, "Create directory operation failed.");
				}
			}
		});
		commands.add(new FTPCommand("rmd", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				File rt = isAbsolute(line) ? new File(focus.root) : new File(focus.root, focus.cwd);
				File f = new File(rt, line);
				if (!f.getAbsolutePath().startsWith(focus.root)) {
					focus.writeLine(550, "Remove directory operation failed.");
					return;
				}
				if (f.isFile()) f = f.getParentFile();
				if (f.getParentFile() != null && f.getParentFile().exists() && f.getParentFile().canWrite()) {
					f.delete();
					if (!f.exists()) focus.writeLine(250, "Remove directory operation successful.");
					else focus.writeLine(550, "Remove directory operation failed.");
				}else {
					focus.writeLine(550, "Remove directory operation failed.");
				}
			}
		});
		commands.add(new FTPCommand("help", 0, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				focus.writeBMLine(214, "The following commands are recognized.");
				focus.writeMMLine("ABOR ACCT ALLO APPE CDUP CWD  DELE HELP LIST MDTM MKD  MODE NLST NOOP");
				focus.writeMMLine("PASS PASV PORT PWD  QUIT REIN REST RETR RMD  RNFR RNTO SITE SMNT STAT");
				focus.writeMMLine("STOR STOU STRU SYST TYPE USER");
				focus.writeLine(214, "Help OK.");
			}
		});
		commands.add(new FTPCommand("site", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				focus.writeLine(500, "Unknown SITE command.");
			}
		});
		commands.add(new FTPCommand("syst", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				focus.writeLine(215, "UNIX Type: L8"); // even on windows, we use a unix-like system for chrooting.
			}
		});
		commands.add(new FTPCommand("pwd", 1, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				focus.writeLine(257, "\"" + focus.cwd + "\"");
			}
		});
		commands.add(new FTPCommand("stat", 0, 100) {
			public void run(FTPWork focus, String line) throws IOException {
				focus.writeBMLine(211, "FTP server status:");
				focus.writeMMLine("Connected to " + focus.s.getInetAddress().getHostAddress());
				focus.writeMMLine(focus.auth ? ("Logged in as " + focus.user) : "Not logged in");
				focus.writeMMLine("TYPE: " + (focus.type == FTPType.ASCII ? "ASCII" : "BINARY"));
				focus.writeMMLine("No session bandwidth limit");
				focus.writeMMLine("Session timeout in seconds is 300");
				focus.writeMMLine("Control connection is plain text");
				focus.writeMMLine("Data connections will be plain text");
				focus.writeMMLine("At session startup, client count was 1");
				focus.writeMMLine("Avuna HTTPD " + AvunaHTTPD.VERSION);
				focus.writeLine(211, "End of status");
			}
		});
	}
	
	public static boolean isAbsolute(String line) {
		return line.startsWith("/");
	}
	
	public static String chroot(String root, String abs) {
		if (!abs.startsWith(root)) {
			return null;
		}
		String nabs = abs.substring(root.length());
		if (AvunaHTTPD.windows) {
			nabs = nabs.replace("\\", "/");
			if (!nabs.startsWith("/")) nabs = "/" + nabs;
		}else {
			if (!nabs.startsWith("/")) nabs = "/" + nabs;
		}
		return nabs;
	}
}
