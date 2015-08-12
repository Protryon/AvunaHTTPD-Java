/* Avuna HTTPD - General Server Applications Copyright (C) 2015 Maxwell Bruce This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>. */

package org.avuna.httpd.util;

import java.io.File;
import org.avuna.httpd.AvunaHTTPD;

/** Loads native system methods via JNI.
 * 
 * @author Max
 * @see FileManager#getBaseFile(String) */
public abstract class CLib {
	
	public static native int socket(int domain, int type, int protocol);
	
	public static native int bindUnix(int sockfd, String path);
	
	public static native int bindTCP(int sockfd, String ip, int port);
	
	public static native int listen(int sockfd, int backlog);
	
	public static native String acceptUnix(int sockfd);
	
	public static native String acceptTCP(int sockfd);
	
	public static native byte[] read(int sockfd, int size);
	
	public static native int write(int sockfd, byte[] ba);
	
	public static native int connect(int sockfd, String sockaddr);
	
	public static native int close(int sockfd);
	
	public static native int umask(int umask);
	
	public static native int setuid(int uid);
	
	public static native int setgid(int gid);
	
	public static native int getuid();
	
	public static native int getgid();
	
	public static native int seteuid(int uid);
	
	public static native int geteuid();
	
	public static native int setegid(int uid);
	
	public static native int getegid();
	
	public static native int fflush(int sockfd);
	
	public static native String stat(String path);
	
	public static native int readlink(String path, byte[] buf);
	
	// dont use unless verified not symlink, otherwise escalation ensues.
	public static native int chmod(String path, int chmod);
	
	public static native int lchown(String path, int uid, int gid);
	
	public static native int available(int sockfd);
	
	public static native int errno();
	
	public static native int[] poll(int[] sockfds);
	
	public static native int hasGNUTLS();
	
	public static boolean failed = false;
	
	static {
		if (AvunaHTTPD.windows) {
			failed = true;
		}else {
			String jvma = System.getProperty("sun.arch.data.model");
			String arch = System.getProperty("os.arch");
			String va = null;
			if (jvma.equals("32")) {
				if (arch.equals("i386") || arch.equals("amd64")) va = "i386";
				if (arch.equals("amd64")) AvunaHTTPD.logger.log("[WARNING] You are running a 32-bit JVM on a 64-bit Machine!");
			}else if (jvma.equals("64")) {
				if (arch.equals("amd64")) va = "amd64";
			}
			if (va != null) {
				File af = new File(AvunaHTTPD.fileManager.getBaseFile("jni"), va);
				if (va.equals("amd64")) {
					System.load(new File(af, "libffi.so.6").getAbsolutePath());
					System.load(new File(af, "libtasn1.so.6").getAbsolutePath());
					System.load(new File(af, "libnettle.so.6").getAbsolutePath());
					System.load(new File(af, "libgmp.so.10").getAbsolutePath());
					System.load(new File(af, "libhogweed.so.4").getAbsolutePath());
					System.load(new File(af, "libp11-kit.so.0").getAbsolutePath());
					System.load(new File(af, "libgnutls.so.28").getAbsolutePath());
				}
				System.load(new File(af, "libAvunaHTTPD_JNI.so").getAbsolutePath());
			}else {
				AvunaHTTPD.logger.logError("[ERROR] JNI Loading failed, we could not find a library for your CPU Architecture.");
				failed = true;
			}
		}
	}
}
