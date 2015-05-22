#include <jni.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/un.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <string.h>
#include <fcntl.h>

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    socket
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_socket(JNIEnv * this, jclass cls, jint domain, jint type, jint protocol) {
	return socket(domain, type, protocol);
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    bind
 * Signature: (ILjava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_bind(JNIEnv * this, jclass cls, jint sockfd, jstring path) {
	struct sockaddr_un sun;
	sun.sun_family = 1;
	const char *npath = (*this)->GetStringUTFChars(this, path, 0);
	strcpy(sun.sun_path, npath);
	int i = bind(sockfd, (struct sockaddr *)&sun, sizeof(sun));
	(*this)->ReleaseStringUTFChars(this, path, 0);
	return i;
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    listen
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_listen(JNIEnv * this, jclass cls, jint sockfd, jint backlog) {
	return listen(sockfd, backlog);
}

char* itoa(int val, int base){

	static char buf[32] = {0};

	int i = 30;

	for(; val && i ; --i, val /= base)

		buf[i] = "0123456789abcdef"[val % base];

	return &buf[i+1];

}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    accept
 * Signature: (ILjava/lang/String;I)I
 */
JNIEXPORT jstring JNICALL Java_org_avuna_httpd_util_CLib_accept(JNIEnv * this, jclass cls, jint sockfd) {
	struct sockaddr_un sun;
	sun.sun_family = 1;
	char *fpath = malloc(32);
	if(fpath == NULL) {
		return NULL;
	}
	socklen_t slt = sizeof(sun);
	int i = accept(sockfd, (struct sockaddr *)&sun, &slt);
	if(i < 0) {
		fpath = malloc(8);
		if(fpath == NULL) {
			return NULL;
		}
		strcpy(fpath, "-1/null");
		//*fpath = "-1/null";
	}else{
		free(fpath);
		fpath = itoa(i, 10);
		char *cr1;
		cr1 = malloc(strlen(fpath) + 1 + strlen((char *)&sun.sun_path) + 1);
		if(cr1 == NULL) {
			return NULL;
		}
		strcpy(cr1, fpath);
		strcat(cr1, "/");
		strcat(cr1, (char *)&sun.sun_path);
		fpath = cr1;
	}
	jstring js = (*this)->NewStringUTF(this, fpath);
	free(fpath);
	return js;
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    read
 * Signature: (I[BI)I
 */
JNIEXPORT jbyteArray JNICALL Java_org_avuna_httpd_util_CLib_read(JNIEnv * this, jclass cls, jint sockfd, jint size) {
	jbyte* ra = malloc(size);
	memset(ra, 0, size);
	int i = read(sockfd, ra, size);
	if(i < 0) {
		i = 0;
	}
	jbyteArray f = (*this)->NewByteArray(this, i);
	if (f == NULL) {
		return NULL;
	}
	if(i >= 0) {
		(*this)->SetByteArrayRegion(this, f, 0, i, (jbyte*)ra);
	}
	free(ra);
	return f;
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    write
 * Signature: (I[BI)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_write(JNIEnv * this, jclass cls, jint sockfd, jbyteArray buf) {
	jbyte* jb = (*this)->GetByteArrayElements(this, buf, 0);
	int i = write(sockfd, jb, (*this)->GetArrayLength(this, buf));
	(*this)->ReleaseByteArrayElements(this, buf, jb, 0);
	return i;
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    connect
 * Signature: (I[BI)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_connect(JNIEnv * this, jclass cls, jint sockfd, jstring path) {
	const char *npath = (*this)->GetStringUTFChars(this, path, 0);
	struct sockaddr_un sun;
	sun.sun_family = AF_UNIX;
	strncpy(sun.sun_path, npath, 108);
	int i = connect((int)sockfd, (struct sockaddr *)&sun, sizeof(sun));
	(*this)->ReleaseStringUTFChars(this, path, 0);
	return i;
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    close
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_close(JNIEnv * this, jclass cls, jint sockfd) {
	return close(sockfd);
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    umask
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_umask(JNIEnv * this, jclass cls, jint um) {
	return umask(um);
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    setuid
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_setuid(JNIEnv * this, jclass cls, jint uid) {
	return setuid(uid);
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    setgid
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_setgid(JNIEnv * this, jclass cls, jint gid) {
	return setgid(gid);
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    getuid
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_getuid(JNIEnv * this, jclass cls) {
	return getuid();
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    getgid
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_getgid(JNIEnv * this, jclass cls) {
	return getgid();
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    seteuid
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_seteuid(JNIEnv * this, jclass cls, jint euid) {
	return seteuid(euid);
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    geteuid
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_geteuid(JNIEnv * this, jclass cls) {
	return geteuid();
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    setegid
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_setegid(JNIEnv * this, jclass cls, jint egid) {
	return setegid(egid);
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    getegid
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_getegid(JNIEnv * this, jclass cls) {
	return getegid();
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    fflush
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_fflush(JNIEnv * this, jclass env, jint sockfd) {
	return fflush((FILE *)&sockfd);
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    __xstat64
 * Signature: (ILjava/lang/String;[B)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib__1_1xstat64(JNIEnv * this, jclass cls, jstring path, jbyteArray buf) {
	jbyte* jb = (*this)->GetByteArrayElements(this, buf, 0);
	const char *npath = (*this)->GetStringUTFChars(this, path, 0);
	int i = stat(npath, (void *)jb);
	(*this)->ReleaseStringUTFChars(this, path, 0);
	(*this)->ReleaseByteArrayElements(this, buf, jb, 0);
	return i;
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    readlink
 * Signature: (Ljava/lang/String;[BI)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_readlink(JNIEnv * this, jclass cls, jstring path, jbyteArray buf) {
	jbyte* jb = (*this)->GetByteArrayElements(this, buf, 0);
	const char *npath = (*this)->GetStringUTFChars(this, path, 0);
	int i = readlink(npath, jb, (*this)->GetArrayLength(this, buf));
	(*this)->ReleaseStringUTFChars(this, path, 0);
	(*this)->ReleaseByteArrayElements(this, buf, jb, 0);
	return i;
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    chmod
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_chmod(JNIEnv * this, jclass cls, jstring path, jint ch) {
	const char *npath = (*this)->GetStringUTFChars(this, path, 0);
	int i = chmod(npath, ch);
	(*this)->ReleaseStringUTFChars(this, path, 0);
	return i;
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    lchown
 * Signature: (Ljava/lang/String;II)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_lchown(JNIEnv * this, jclass cls, jstring path, jint uid, jint gid) {
	const char *npath = (*this)->GetStringUTFChars(this, path, 0);
	int i = lchown(npath, uid, gid);
	(*this)->ReleaseStringUTFChars(this, path, 0);
	return i;
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    available
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_available(JNIEnv * this, jclass cls, jint sockfd) {
	jint z = 0;
	int i = ioctl(sockfd, FIONREAD, &z);
	return z;
}

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    errno
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_errno(JNIEnv * this, jclass cls) {
	return errno;
}
