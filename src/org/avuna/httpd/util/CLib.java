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
	
	public static String errname(int errno) {
		switch (errno) {
			case 1:
				return "Operation not permitted";
			case 2:
				return "No such file or directory";
			case 3:
				return "No such process";
			case 4:
				return "Interrupted system call";
			case 5:
				return "I/O error";
			case 6:
				return "No such device or address";
			case 7:
				return "Argument list too long";
			case 8:
				return "Exec format error";
			case 9:
				return "Bad file number";
			case 10:
				return "No child processes";
			case 11:
				return "Try again";
			case 12:
				return "Out of memory";
			case 13:
				return "Permission denied";
			case 14:
				return "Bad address";
			case 15:
				return "Block device required";
			case 16:
				return "Device or resource busy";
			case 17:
				return "File exists";
			case 18:
				return "Cross-device link";
			case 19:
				return "No such device";
			case 20:
				return "Not a directory";
			case 21:
				return "Is a directory";
			case 22:
				return "Invalid argument";
			case 23:
				return "File table overflow";
			case 24:
				return "Too many open files";
			case 25:
				return "Not a typewriter";
			case 26:
				return "Text file busy";
			case 27:
				return "File too large";
			case 28:
				return "No space left on device";
			case 29:
				return "Illegal seek";
			case 30:
				return "Read-only file system";
			case 31:
				return "Too many links";
			case 32:
				return "Broken pipe";
			case 33:
				return "Math argument out of domain of func";
			case 34:
				return "Math result not representable";
			case 35:
				return "Resource deadlock would occur";
			case 36:
				return "File name too long";
			case 37:
				return "No record locks available";
			case 38:
				return "Function not implemented";
			case 39:
				return "Directory not empty";
			case 40:
				return "Too many symbolic links encountered";
			case 41:
				return null;
			case 42:
				return "No message of desired type";
			case 43:
				return "Identifier removed";
			case 44:
				return "Channel number out of range";
			case 45:
				return "Level 2 not synchronized";
			case 46:
				return "Level 3 halted";
			case 47:
				return "Level 3 reset";
			case 48:
				return "Link number out of range";
			case 49:
				return "Protocol driver not attached";
			case 50:
				return "No CSI structure available";
			case 51:
				return "Level 2 halted";
			case 52:
				return "Invalid exchange";
			case 53:
				return "Invalid request descriptor";
			case 54:
				return "Exchange full";
			case 55:
				return "No anode";
			case 56:
				return "Invalid request code";
			case 57:
				return "Invalid slot";
			case 58:
				return null;
			case 59:
				return "Bad font file format";
			case 60:
				return "Device not a stream";
			case 61:
				return "No data available";
			case 62:
				return "Timer expired";
			case 63:
				return "Out of streams resources";
			case 64:
				return "Machine is not on the network";
			case 65:
				return "Package not installed";
			case 66:
				return "Object is remote";
			case 67:
				return "Link has been severed";
			case 68:
				return "Advertise error";
			case 69:
				return "Srmount error";
			case 70:
				return "Communication error on send";
			case 71:
				return "Protocol error";
			case 72:
				return "Multihop attempted";
			case 73:
				return "RFS specific error";
			case 74:
				return "Not a data message";
			case 75:
				return "Value too large for defined data type";
			case 76:
				return "Name not unique on network";
			case 77:
				return "File descriptor in bad state";
			case 78:
				return "Remote address changed";
			case 79:
				return "Can not access a needed shared library";
			case 80:
				return "Accessing a corrupted shared library";
			case 81:
				return ".lib section in a.out corrupted";
			case 82:
				return "Attempting to link in too many shared libraries";
			case 83:
				return "Cannot exec a shared library directly";
			case 84:
				return "Illegal byte sequence";
			case 85:
				return "Interrupted system call should be restarted";
			case 86:
				return "Streams pipe error";
			case 87:
				return "Too many users";
			case 88:
				return "Socket operation on non-socket";
			case 89:
				return "Destination address required";
			case 90:
				return "Message too long";
			case 91:
				return "Protocol wrong type for socket";
			case 92:
				return "Protocol not available";
			case 93:
				return "Protocol not supported";
			case 94:
				return "Socket type not supported";
			case 95:
				return "Operation not supported on transport endpoint";
			case 96:
				return "Protocol family not supported";
			case 97:
				return "Address family not supported by protocol";
			case 98:
				return "Address already in use";
			case 99:
				return "Cannot assign requested address";
			case 100:
				return "Network is down";
			case 101:
				return "Network is unreachable";
			case 102:
				return "Network dropped connection because of reset";
			case 103:
				return "Software caused connection abort";
			case 104:
				return "Connection reset by peer";
			case 105:
				return "No buffer space available";
			case 106:
				return "Transport endpoint is already connected";
			case 107:
				return "Transport endpoint is not connected";
			case 108:
				return "Cannot send after transport endpoint shutdown";
			case 109:
				return "Too many references: cannot splice";
			case 110:
				return "Connection timed out";
			case 111:
				return "Connection refused";
			case 112:
				return "Host is down";
			case 113:
				return "No route to host";
			case 114:
				return "Operation already in progress";
			case 115:
				return "Operation now in progress";
			case 116:
				return "Stale NFS file handle";
			case 117:
				return "Structure needs cleaning";
			case 118:
				return "Not a XENIX named type file";
			case 119:
				return "No XENIX semaphores available";
			case 120:
				return "Is a named type file";
			case 121:
				return "Remote I/O error";
			case 122:
				return "Quota exceeded";
			case 123:
				return "No medium found";
			case 124:
				return "Wrong medium type";
			case 125:
				return "Operation Canceled";
			case 126:
				return "Required key not available";
			case 127:
				return "Key has expired";
			case 128:
				return "Key has been revoked";
			case 129:
				return "Key was rejected by service";
			case 130:
				return "Owner died";
			case 131:
				return "State not recoverable";
			default:
				return null;
		}
	}
	
	public static native int[] poll(int[] sockfds);
	
	public static native int hasGNUTLS();
	
	public static boolean failed = false;
	
	static {
		if (AvunaHTTPD.windows || AvunaHTTPD.legacy) {
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
