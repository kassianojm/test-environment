/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_zeromq_ZMQ_Socket */

#ifndef _Included_org_zeromq_ZMQ_Socket
#define _Included_org_zeromq_ZMQ_Socket
#ifdef __cplusplus
extern "C" {
#endif
#undef org_zeromq_ZMQ_Socket_HWM
#define org_zeromq_ZMQ_Socket_HWM 1L
#undef org_zeromq_ZMQ_Socket_SWAP
#define org_zeromq_ZMQ_Socket_SWAP 3L
#undef org_zeromq_ZMQ_Socket_AFFINITY
#define org_zeromq_ZMQ_Socket_AFFINITY 4L
#undef org_zeromq_ZMQ_Socket_IDENTITY
#define org_zeromq_ZMQ_Socket_IDENTITY 5L
#undef org_zeromq_ZMQ_Socket_SUBSCRIBE
#define org_zeromq_ZMQ_Socket_SUBSCRIBE 6L
#undef org_zeromq_ZMQ_Socket_UNSUBSCRIBE
#define org_zeromq_ZMQ_Socket_UNSUBSCRIBE 7L
#undef org_zeromq_ZMQ_Socket_RATE
#define org_zeromq_ZMQ_Socket_RATE 8L
#undef org_zeromq_ZMQ_Socket_RECOVERY_IVL
#define org_zeromq_ZMQ_Socket_RECOVERY_IVL 9L
#undef org_zeromq_ZMQ_Socket_MCAST_LOOP
#define org_zeromq_ZMQ_Socket_MCAST_LOOP 10L
#undef org_zeromq_ZMQ_Socket_SNDBUF
#define org_zeromq_ZMQ_Socket_SNDBUF 11L
#undef org_zeromq_ZMQ_Socket_RCVBUF
#define org_zeromq_ZMQ_Socket_RCVBUF 12L
#undef org_zeromq_ZMQ_Socket_RCVMORE
#define org_zeromq_ZMQ_Socket_RCVMORE 13L
#undef org_zeromq_ZMQ_Socket_FD
#define org_zeromq_ZMQ_Socket_FD 14L
#undef org_zeromq_ZMQ_Socket_EVENTS
#define org_zeromq_ZMQ_Socket_EVENTS 15L
#undef org_zeromq_ZMQ_Socket_TYPE
#define org_zeromq_ZMQ_Socket_TYPE 16L
#undef org_zeromq_ZMQ_Socket_LINGER
#define org_zeromq_ZMQ_Socket_LINGER 17L
#undef org_zeromq_ZMQ_Socket_RECONNECT_IVL
#define org_zeromq_ZMQ_Socket_RECONNECT_IVL 18L
#undef org_zeromq_ZMQ_Socket_BACKLOG
#define org_zeromq_ZMQ_Socket_BACKLOG 19L
#undef org_zeromq_ZMQ_Socket_RECONNECT_IVL_MAX
#define org_zeromq_ZMQ_Socket_RECONNECT_IVL_MAX 21L
#undef org_zeromq_ZMQ_Socket_MAXMSGSIZE
#define org_zeromq_ZMQ_Socket_MAXMSGSIZE 22L
#undef org_zeromq_ZMQ_Socket_SNDHWM
#define org_zeromq_ZMQ_Socket_SNDHWM 23L
#undef org_zeromq_ZMQ_Socket_RCVHWM
#define org_zeromq_ZMQ_Socket_RCVHWM 24L
#undef org_zeromq_ZMQ_Socket_MULTICAST_HOPS
#define org_zeromq_ZMQ_Socket_MULTICAST_HOPS 25L
#undef org_zeromq_ZMQ_Socket_RCVTIMEO
#define org_zeromq_ZMQ_Socket_RCVTIMEO 27L
#undef org_zeromq_ZMQ_Socket_SNDTIMEO
#define org_zeromq_ZMQ_Socket_SNDTIMEO 28L
#undef org_zeromq_ZMQ_Socket_IPV4ONLY
#define org_zeromq_ZMQ_Socket_IPV4ONLY 31L
#undef org_zeromq_ZMQ_Socket_LAST_ENDPOINT
#define org_zeromq_ZMQ_Socket_LAST_ENDPOINT 32L
#undef org_zeromq_ZMQ_Socket_ROUTER_MANDATORY
#define org_zeromq_ZMQ_Socket_ROUTER_MANDATORY 33L
#undef org_zeromq_ZMQ_Socket_KEEPALIVE
#define org_zeromq_ZMQ_Socket_KEEPALIVE 34L
#undef org_zeromq_ZMQ_Socket_KEEPALIVECNT
#define org_zeromq_ZMQ_Socket_KEEPALIVECNT 35L
#undef org_zeromq_ZMQ_Socket_KEEPALIVEIDLE
#define org_zeromq_ZMQ_Socket_KEEPALIVEIDLE 36L
#undef org_zeromq_ZMQ_Socket_KEEPALIVEINTVL
#define org_zeromq_ZMQ_Socket_KEEPALIVEINTVL 37L
#undef org_zeromq_ZMQ_Socket_IMMEDIATE
#define org_zeromq_ZMQ_Socket_IMMEDIATE 39L
#undef org_zeromq_ZMQ_Socket_XPUB_VERBOSE
#define org_zeromq_ZMQ_Socket_XPUB_VERBOSE 40L
#undef org_zeromq_ZMQ_Socket_PLAIN_SERVER
#define org_zeromq_ZMQ_Socket_PLAIN_SERVER 44L
#undef org_zeromq_ZMQ_Socket_PLAIN_USERNAME
#define org_zeromq_ZMQ_Socket_PLAIN_USERNAME 45L
#undef org_zeromq_ZMQ_Socket_PLAIN_PASSWORD
#define org_zeromq_ZMQ_Socket_PLAIN_PASSWORD 46L
#undef org_zeromq_ZMQ_Socket_CURVE_SERVER
#define org_zeromq_ZMQ_Socket_CURVE_SERVER 47L
#undef org_zeromq_ZMQ_Socket_CURVE_PUBLICKEY
#define org_zeromq_ZMQ_Socket_CURVE_PUBLICKEY 48L
#undef org_zeromq_ZMQ_Socket_CURVE_SECRETKEY
#define org_zeromq_ZMQ_Socket_CURVE_SECRETKEY 49L
#undef org_zeromq_ZMQ_Socket_CURVE_SERVERKEY
#define org_zeromq_ZMQ_Socket_CURVE_SERVERKEY 50L
#undef org_zeromq_ZMQ_Socket_PROBE_ROUTER
#define org_zeromq_ZMQ_Socket_PROBE_ROUTER 51L
#undef org_zeromq_ZMQ_Socket_REQ_CORRELATE
#define org_zeromq_ZMQ_Socket_REQ_CORRELATE 52L
#undef org_zeromq_ZMQ_Socket_REQ_RELAXED
#define org_zeromq_ZMQ_Socket_REQ_RELAXED 53L
#undef org_zeromq_ZMQ_Socket_CONFLATE
#define org_zeromq_ZMQ_Socket_CONFLATE 54L
#undef org_zeromq_ZMQ_Socket_ZAP_DOMAIN
#define org_zeromq_ZMQ_Socket_ZAP_DOMAIN 55L
#undef org_zeromq_ZMQ_Socket_ROUTER_HANDOVER
#define org_zeromq_ZMQ_Socket_ROUTER_HANDOVER 56L
#undef org_zeromq_ZMQ_Socket_GSSAPI_SERVER
#define org_zeromq_ZMQ_Socket_GSSAPI_SERVER 62L
#undef org_zeromq_ZMQ_Socket_GSSAPI_PRINCIPAL
#define org_zeromq_ZMQ_Socket_GSSAPI_PRINCIPAL 63L
#undef org_zeromq_ZMQ_Socket_GSSAPI_SERVICE_PRINCIPAL
#define org_zeromq_ZMQ_Socket_GSSAPI_SERVICE_PRINCIPAL 64L
#undef org_zeromq_ZMQ_Socket_GSSAPI_PLAINTEXT
#define org_zeromq_ZMQ_Socket_GSSAPI_PLAINTEXT 65L
/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    nativeInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_zeromq_ZMQ_00024Socket_nativeInit
  (JNIEnv *, jclass);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    bind
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_zeromq_ZMQ_00024Socket_bind
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    unbind
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_zeromq_ZMQ_00024Socket_unbind
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    connect
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_zeromq_ZMQ_00024Socket_connect
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    disconnect
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_zeromq_ZMQ_00024Socket_disconnect
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    monitor
 * Signature: (Ljava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_zeromq_ZMQ_00024Socket_monitor
  (JNIEnv *, jobject, jstring, jint);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    send
 * Signature: ([BIII)Z
 */
JNIEXPORT jboolean JNICALL Java_org_zeromq_ZMQ_00024Socket_send
  (JNIEnv *, jobject, jbyteArray, jint, jint, jint);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    sendZeroCopy
 * Signature: (Ljava/nio/ByteBuffer;II)Z
 */
JNIEXPORT jboolean JNICALL Java_org_zeromq_ZMQ_00024Socket_sendZeroCopy
  (JNIEnv *, jobject, jobject, jint, jint);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    sendByteBuffer
 * Signature: (Ljava/nio/ByteBuffer;I)I
 */
JNIEXPORT jint JNICALL Java_org_zeromq_ZMQ_00024Socket_sendByteBuffer
  (JNIEnv *, jobject, jobject, jint);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    recv
 * Signature: (I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_zeromq_ZMQ_00024Socket_recv__I
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    recv
 * Signature: ([BIII)I
 */
JNIEXPORT jint JNICALL Java_org_zeromq_ZMQ_00024Socket_recv___3BIII
  (JNIEnv *, jobject, jbyteArray, jint, jint, jint);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    recvZeroCopy
 * Signature: (Ljava/nio/ByteBuffer;II)I
 */
JNIEXPORT jint JNICALL Java_org_zeromq_ZMQ_00024Socket_recvZeroCopy
  (JNIEnv *, jobject, jobject, jint, jint);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    recvByteBuffer
 * Signature: (Ljava/nio/ByteBuffer;I)I
 */
JNIEXPORT jint JNICALL Java_org_zeromq_ZMQ_00024Socket_recvByteBuffer
  (JNIEnv *, jobject, jobject, jint);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    construct
 * Signature: (Lorg/zeromq/ZMQ/Context;I)V
 */
JNIEXPORT void JNICALL Java_org_zeromq_ZMQ_00024Socket_construct
  (JNIEnv *, jobject, jobject, jint);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    destroy
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_zeromq_ZMQ_00024Socket_destroy
  (JNIEnv *, jobject);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    getLongSockopt
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_org_zeromq_ZMQ_00024Socket_getLongSockopt
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    getBytesSockopt
 * Signature: (I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_zeromq_ZMQ_00024Socket_getBytesSockopt
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    setLongSockopt
 * Signature: (IJ)V
 */
JNIEXPORT void JNICALL Java_org_zeromq_ZMQ_00024Socket_setLongSockopt
  (JNIEnv *, jobject, jint, jlong);

/*
 * Class:     org_zeromq_ZMQ_Socket
 * Method:    setBytesSockopt
 * Signature: (I[B)V
 */
JNIEXPORT void JNICALL Java_org_zeromq_ZMQ_00024Socket_setBytesSockopt
  (JNIEnv *, jobject, jint, jbyteArray);

#ifdef __cplusplus
}
#endif
#endif
