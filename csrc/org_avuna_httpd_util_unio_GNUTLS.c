// provided by IDE or manually defined.
#ifdef BIT64
#include <jni.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <gnutls/gnutls.h>
#include <string.h>

static gnutls_dh_params_t dh_params;

struct cert {
	gnutls_certificate_credentials_t cert;
	gnutls_priority_t priority;
};

JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_unio_GNUTLS_global_init(JNIEnv * this, jclass cls) {
	gnutls_global_init();
	unsigned int bits = gnutls_sec_param_to_pk_bits(GNUTLS_PK_DH, GNUTLS_SEC_PARAM_LEGACY);
	gnutls_dh_params_init(&dh_params);
	gnutls_dh_params_generate2(dh_params, bits);
	return 0;
}

JNIEXPORT jlong JNICALL Java_org_avuna_httpd_util_unio_GNUTLS_load_cert(JNIEnv * this, jclass cls, jstring ca, jstring crl, jstring cert, jstring key) {
	struct cert* oc = malloc(sizeof( struct cert));
	memset(oc, 0, sizeof(struct cert));
	gnutls_certificate_allocate_credentials(&oc->cert);
	const char *caj = (*this)->GetStringUTFChars(this, ca, 0);
	gnutls_certificate_set_x509_trust_file(oc->cert, caj, GNUTLS_X509_FMT_PEM);
	(*this)->ReleaseStringUTFChars(this, ca, 0);
	const char *crlj = (*this)->GetStringUTFChars(this, crl, 0);
	gnutls_certificate_set_x509_crl_file(oc->cert, crlj, GNUTLS_X509_FMT_PEM);
	(*this)->ReleaseStringUTFChars(this, crl, 0);
	const char *certj = (*this)->GetStringUTFChars(this, cert, 0);
	const char *keyj = (*this)->GetStringUTFChars(this, key, 0);
	int e1 = gnutls_certificate_set_x509_key_file(oc->cert, certj, keyj, GNUTLS_X509_FMT_PEM);
	(*this)->ReleaseStringUTFChars(this, key, 0);
	(*this)->ReleaseStringUTFChars(this, cert, 0);
	if(e1 < 0) {
		return 0;
	}
	gnutls_certificate_set_ocsp_status_request_file(oc->cert, "ocsp-status.der", 0);
	gnutls_priority_init(&oc->priority, "PERFORMANCE:%SERVER_PRECEDENCE", NULL);
	gnutls_certificate_set_dh_params(oc->cert, dh_params);
	return (jlong)oc;
}

JNIEXPORT jlong JNICALL Java_org_avuna_httpd_util_unio_GNUTLS_preaccept(JNIEnv * this, jclass cls, jlong cert) {
	struct cert* oc = (struct cert*)cert;
	gnutls_session_t *session = malloc(sizeof(gnutls_session_t));
	memset(session, 0, sizeof(gnutls_session_t));
	gnutls_init(session, GNUTLS_SERVER | GNUTLS_NONBLOCK);
	gnutls_priority_set(*session, oc->priority);
	gnutls_credentials_set(*session, GNUTLS_CRD_CERTIFICATE, oc->cert);
	gnutls_certificate_server_set_request(*session, GNUTLS_CERT_IGNORE);
	return (jlong)session;
}

JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_unio_GNUTLS_postaccept(JNIEnv * this, jclass cls, jlong cert, jlong session, jint sockfd) {
	gnutls_session_t sessiond = *((gnutls_session_t*)session);
	gnutls_transport_set_int(sessiond, sockfd);
	int ret = 0;
	do {
		ret = gnutls_handshake(sessiond);
	}while (ret < 0 && gnutls_error_is_fatal(ret) == 0);
	return ret;
}

JNIEXPORT jbyteArray JNICALL Java_org_avuna_httpd_util_unio_GNUTLS_read(JNIEnv * this, jclass cls, jlong session, jint size) {
	gnutls_session_t sessiond = *((gnutls_session_t*)session);
	jbyte* ra = malloc(size);
	memset(ra, 0, size);
	int i = gnutls_record_recv(sessiond, ra, size);
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

JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_unio_GNUTLS_write(JNIEnv * this, jclass cls, jlong session, jbyteArray ba) {
	gnutls_session_t sessiond = *((gnutls_session_t*)session);
	jbyte* jb = (*this)->GetByteArrayElements(this, ba, 0);
	int i = gnutls_record_send(sessiond, jb, (*this)->GetArrayLength(this, ba));
	(*this)->ReleaseByteArrayElements(this, ba, jb, 0);
	return i;
}

JNIEXPORT jint JNICALL Java_org_avuna_httpd_util_unio_GNUTLS_close(JNIEnv * this, jclass cls, jlong session) {
	gnutls_deinit(*((gnutls_session_t *)session));
	return 0;
}
#endif
