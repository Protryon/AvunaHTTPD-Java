package org.avuna.httpd.util;

import java.io.File;
import org.avuna.httpd.AvunaHTTPD;
import org.avuna.httpd.util.CLib.bap;

public class SafeMode {
	public static boolean isSymlink(File f) {
		if (AvunaHTTPD.windows) return false;
		byte[] rb = f.getAbsolutePath().getBytes();
		CLib.bap bap = new CLib.bap(rb.length);
		System.arraycopy(rb, 0, bap.array, 0, rb.length);
		return isSymlink(bap);
	}
	
	private static boolean isSymlink(bap f) {
		bap bap = new bap(255);
		int length = CLib.INSTANCE.readlink(f, bap, 255);
		return length >= 0;
	}
	
	public static void setPerms(File root, int uid, int gid, int chmod) {
		// Logger.log("Setting " + root.getAbsolutePath() + " to " + uid + ":" + gid + " chmod " + chmod);
		byte[] rb = root.getAbsolutePath().getBytes();
		CLib.bap bap = new CLib.bap(rb.length);
		System.arraycopy(rb, 0, bap.array, 0, rb.length);
		if (isSymlink(bap)) return;
		int ch = CLib.INSTANCE.chmod(bap, chmod);
		// Logger.log("lchmod returned: " + ch + (ch == -1 ? " error code: " + Native.getLastError() : ""));
		CLib.INSTANCE.lchown(bap, uid, gid);
	}
	
	public static void setPerms(File root, int uid, int gid) {
		setPerms(root, uid, gid, false);
	}
	
	// should run as root
	private static void setPerms(File root, int uid, int gid, boolean recursed) {
		CLib.INSTANCE.umask(0000);
		setPerms(root, uid, gid, 0700);
		for (File f : root.listFiles()) {
			if (f.isDirectory()) {
				setPerms(f, uid, gid, true);
			}else {
				if (uid == 0 && gid == 0) {
					setPerms(f, 0, 0, 0700);
				}else {
					String rn = f.getName();
					if (!recursed && (rn.equals("avuna.jar") || rn.equals("main.cfg"))) {
						setPerms(f, 0, gid, 0640);
					}else if (!recursed && (rn.equals("kill.sh") || rn.equals("run.sh") || rn.equals("restart.sh") || rn.equals("cmd.sh"))) {
						setPerms(f, 0, 0, 0700);
					}else {
						setPerms(f, uid, gid, 0700);
					}
				}
			}
		}
		CLib.INSTANCE.umask(0077);
	}
}
