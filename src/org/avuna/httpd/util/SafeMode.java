package org.avuna.httpd.util;

import java.io.File;
import java.nio.ByteBuffer;
import org.avuna.httpd.AvunaHTTPD;

public class SafeMode {
	public static boolean isSymlink(File f) {
		if (AvunaHTTPD.windows) return false;
		return isSymlink(f.getAbsolutePath(), f.isFile());
	}
	
	private static boolean isHardlink(String f, boolean isFile) {
		if (!isFile) return false; // folders CANNOT be hardlinked, but do return the number of subfolders(+1 or 2)
		byte[] buf = new byte[1024];
		int s = CLib.__xstat64(f, buf);
		if (s == -1) {
			// Logger.log("hle: " + Native.getLastError());
		}
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.order(java.nio.ByteOrder.LITTLE_ENDIAN);
		bb.put(0, buf[13]);
		bb.put(0, buf[14]);
		bb.put(0, buf[15]);
		bb.put(0, buf[16]);
		int hcount = bb.getInt(0);
		// Logger.log(hcount + "");
		return hcount > 1;
	}
	
	private static boolean isSymlink(String f, boolean isFile) {
		byte[] buf = new byte[1024];
		int length = CLib.readlink(f, buf);
		boolean hl = isHardlink(f, isFile);
		// Logger.log("" + (length) + " hl = " + hl);
		return length >= 0 || hl;
	}
	
	public static void setPerms(File root, int uid, int gid, int chmod) { // TODO: block ALL crontab + chroot
		// Logger.log("Setting " + root.getAbsolutePath() + " to " + uid + ":" + gid + " chmod " + chmod);
		// Logger.log(root.getAbsolutePath());
		String ra = root.getAbsolutePath();
		if (isSymlink(ra, root.isFile())) return;
		CLib.umask(0000);
		CLib.chmod(ra, chmod);
		// Logger.log("lchmod returned: " + ch + (ch == -1 ? " error code: " + Native.getLastError() : ""));
		CLib.lchown(ra, uid, gid);
		CLib.umask(0077);
	}
	
	/**
	 * should only be used on an avuna root directory with std perms.
	 * 
	 * @param root
	 * @param uid
	 * @param gid
	 */
	public static void setPerms(File root, int uid, int gid) {
		setPerms(root, uid, gid, false);
	}
	
	// should run as root
	private static void setPerms(File root, int uid, int gid, boolean recursed) {
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
	}
}
