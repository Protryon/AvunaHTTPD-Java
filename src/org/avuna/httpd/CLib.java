package org.avuna.httpd;

import java.util.Arrays;
import java.util.List;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

public interface CLib extends Library {
	CLib INSTANCE = (CLib)Native.loadLibrary((Platform.isWindows() ? "msvcrt" : "c"), CLib.class);
	
	public int socket(int domain, int type, int protocol);
	
	public int bind(int sockfd, sockaddr_un sockaddr, int len);
	
	public int listen(int sockfd, int backlog);
	
	public int accept(int sockfd, sockaddr_un sockaddr, IntByReference len);
	
	public int recv(int sockfd, bap byteArrayPointer, int len, int flags);
	
	public int send(int sockfd, bap byteArrayPointer, int len, int flags);
	
	public int read(int sockfd, bap byteArrayPointer, int len);
	
	public int write(int sockfd, bap byteArrayPointer, int len);
	
	public static class bap extends Structure {
		
		public byte[] array;
		
		public bap(int size) {
			array = new byte[size];
		}
		
		@Override
		protected List getFieldOrder() {
			return Arrays.asList(new String[]{"array"});
		}
		
	}
	
	public int connect(int sockfd, sockaddr_un sockaddr, int len);
	
	public static class sockaddr_un extends Structure {
		
		public short sunfamily = 1;
		public byte[] sunpath = new byte[108]; // must be 108 long.
		
		@Override
		protected List getFieldOrder() {
			return Arrays.asList(new String[]{"sunfamily", "sunpath"});
		}
	}
	
	public int ioctl(int sockfd, int mode, IntByReference count);
	
	public int close(int sockfd);
	
	public int umask(int umask);
	
	public int setuid(int uid);
	
	public int setgid(int gid);
	
	public int getuid();
	
	public int getgid();
	
	public int setsuid(int uid);
	
	public int setsgid(int gid);
	
	public int getsuid();
	
	public int getsgid();
	
	public int fflush(int sockfd);
	
	// chmod/chown directly is a security risk. ex. hardlink to /etc/shadow
	public int lchmod(bap bap, int chmod);
	
	public int lchown(bap bap, int uid, int gid);
}