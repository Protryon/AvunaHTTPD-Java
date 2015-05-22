package org.avuna.httpd.util;

import java.io.File;
import org.avuna.httpd.AvunaHTTPD;

/**
* Loads native system methods via JNI.
* 
* @author Max
* @see FileManager#getBaseFile(String)
*/
public class CLib {
	private CLib() {
	}
	
	public static native int socket(int domain, int type, int protocol);
	
	public static native int bind(int sockfd, String path);
	
	public static native int listen(int sockfd, int backlog);
	
	public static native String accept(int sockfd);
	
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
	
	public static native int __xstat64(String path, byte[] statdump);
	
	public static native int readlink(String path, byte[] buf);
	
	// dont use unless verified not symlink, otherwise escalation ensues.
	public static native int chmod(String path, int chmod);
	
	public static native int lchown(String path, int uid, int gid);
	
	public static native int available(int sockfd);
	
	public static native int errno();
	
	static {
		if (!AvunaHTTPD.windows) {
			System.load(new File(new File(AvunaHTTPD.fileManager.getBaseFile("jni"), System.getProperty("os.arch")), "libAvunaHTTPD_JNI.so").getAbsolutePath());
		}
	}
}
