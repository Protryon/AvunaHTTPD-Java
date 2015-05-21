/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_avuna_httpd_util_CLib */

#ifndef _Included_org_avuna_httpd_util_CLib
#define _Included_org_avuna_httpd_util_CLib
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    socket
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_socket
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    bind
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_bind
  (JNIEnv *, jclass, jint, jstring);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    listen
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_listen
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    accept
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_avuna_httpd_util_CLib_accept
  (JNIEnv *, jclass, jint);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    read
 * Signature: (I[B)I
 */
JNIEXPORT jbyteArray JNICALL Java_org_avuna_httpd_util_CLib_read
  (JNIEnv *, jclass, jint);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    write
 * Signature: (I[B)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_write
  (JNIEnv *, jclass, jint, jbyteArray);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    connect
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_connect
  (JNIEnv *, jclass, jint, jstring);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    close
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_close
  (JNIEnv *, jclass, jint);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    umask
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_umask
  (JNIEnv *, jclass, jint);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    setuid
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_setuid
  (JNIEnv *, jclass, jint);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    setgid
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_setgid
  (JNIEnv *, jclass, jint);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    getuid
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_getuid
  (JNIEnv *, jclass);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    getgid
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_getgid
  (JNIEnv *, jclass);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    seteuid
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_seteuid
  (JNIEnv *, jclass, jint);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    geteuid
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_geteuid
  (JNIEnv *, jclass);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    setegid
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_setegid
  (JNIEnv *, jclass, jint);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    getegid
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_getegid
  (JNIEnv *, jclass);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    fflush
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_fflush
  (JNIEnv *, jclass, jint);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    __xstat64
 * Signature: (Ljava/lang/String;[B)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib__1_1xstat64
  (JNIEnv *, jclass, jstring, jbyteArray);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    readlink
 * Signature: (Ljava/lang/String;[B)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_readlink
  (JNIEnv *, jclass, jstring, jbyteArray);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    chmod
 * Signature: (Ljava/lang/String;I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_chmod
  (JNIEnv *, jclass, jstring, jint);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    lchown
 * Signature: (Ljava/lang/String;II)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_lchown
  (JNIEnv *, jclass, jstring, jint, jint);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    available
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_available
  (JNIEnv *, jclass, jint);

/*
 * Class:     org_avuna_httpd_util_CLib
 * Method:    errno
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_CLib_errno
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
