#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include "openssl/evp.h"
#include "openssl/engine.h"
#include "cryptodev.h"
#include "android/log.h"

#define DEBUG_LOG
#ifdef DEBUG_LOG
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)
#else
#define LOGI(fmt, args...)
#define LOGD(fmt, args...)
#define LOGE(fmt, args...)
#endif

#define BUFSIZE		8192


ENGINE *e = NULL;
EVP_CIPHER_CTX en;
EVP_CIPHER_CTX de;
unsigned char *read_buf = NULL;
const EVP_CIPHER *cipher_type;

static const char *TAG="JNI_cryptodev";
static unsigned char *g_key = NULL;
static unsigned char *g_iv = NULL;




/**
 * Encrypt or decrypt, depending on flag 'should_encrypt'
 */
int en_crypt(FILE *ifp, FILE *ofp, unsigned char *ckey, unsigned char *ivec, JNIEnv *env, jobject thiz, jmethodID mid)
{
	unsigned char *cipher_buf;
	unsigned blocksize;
	int bytes_written = 0;
	int numRead;
	jint nRead;

	blocksize = EVP_CIPHER_CTX_block_size(&en);
	cipher_buf = malloc(BUFSIZE + blocksize);

	LOGD("blocksize=%d", blocksize);

      /* allows reusing of 'e' for multiple encryption cycles */
	if(!EVP_EncryptInit_ex(&en, NULL, NULL, NULL, NULL)){
		LOGE("ERROR in EVP_EncryptInit_ex");
		return 1;
	}

	while (1) {
		// Read in data in blocks until EOF. Update the ciphering with each read.

		numRead = fread(read_buf, sizeof(unsigned char), BUFSIZE, ifp);
		EVP_EncryptUpdate(&en, cipher_buf, &bytes_written, read_buf, numRead);
		fwrite(cipher_buf, sizeof(unsigned char), bytes_written, ofp);
		nRead = numRead;
		(*env)->CallVoidMethod(env, thiz, mid, nRead);
		if (numRead < BUFSIZE) { // EOF
		    break;
		}
	}

	// Now cipher the final block and write it out.

	EVP_EncryptFinal_ex(&en, cipher_buf, &bytes_written);
	fwrite(cipher_buf, sizeof(unsigned char), bytes_written, ofp);

	// Free memory
	free(cipher_buf);


	return 0;
}

/**
 * Encrypt or decrypt, depending on flag 'should_encrypt'
 */
int de_crypt(FILE *ifp, FILE *ofp, unsigned char *ckey, unsigned char *ivec, JNIEnv *env, jobject thiz, jmethodID mid)
{
	unsigned char *cipher_buf;
	unsigned blocksize;
	int bytes_written = 0;
	int numRead;
	jint nRead;


	blocksize = EVP_CIPHER_CTX_block_size(&de);
//	cipher_buf = malloc(BUFSIZE);
	cipher_buf = malloc(BUFSIZE + blocksize);
	LOGD("blocksize=%d", blocksize);

      /* allows reusing of 'e' for multiple encryption cycles */
	if(!EVP_DecryptInit_ex(&de, NULL, NULL, NULL, NULL)){
		LOGE("ERROR in EVP_EncryptInit_ex");
		return 1;
	}

	while (1) {
		// Read in data in blocks until EOF. Update the ciphering with each read.

		numRead = fread(read_buf, sizeof(unsigned char), BUFSIZE, ifp);
		EVP_DecryptUpdate(&de, cipher_buf, &bytes_written, read_buf, numRead);
		fwrite(cipher_buf, sizeof(unsigned char), bytes_written, ofp);
		nRead = numRead;
		(*env)->CallVoidMethod(env, thiz, mid, nRead);
		if (numRead < BUFSIZE) { // EOF
		    break;
		}
	}

	// Now cipher the final block and write it out.

	EVP_DecryptFinal_ex(&de, cipher_buf, &bytes_written);
	fwrite(cipher_buf, sizeof(unsigned char), bytes_written, ofp);


	// Free memory
	free(cipher_buf);


	return 0;
}

/*
 * Class:     com_example_progressbar_CryptoDev
 * Method:    engine_init
 * Signature: ([B[B)I
 */
JNIEXPORT jint JNICALL Java_com_example_progressbar_CryptoDev_engine_1init
  (JNIEnv *env, jobject thiz, jbyteArray key, jbyteArray iv)
{
	jint nRet;
	jbyteArray resultKey;
	jbyteArray resultIv;
	unsigned char v;
	int i = 0;
	jbyte value;
	jsize n;
	jbyte *pbyte;

	n = (*env)->GetArrayLength(env, key);
	pbyte = (*env)->GetByteArrayElements(env, key, 0);

	g_key = (unsigned char*)malloc(n);
	memcpy(g_key, pbyte, n);
	for(i=0;i<n;i++) {
			v = g_key[i];
			(*env)->GetByteArrayRegion(env, key, i, 1, &value);
			LOGE("key[%d]=%d,%d\r\n",i, value, v);
		}

	n = (*env)->GetArrayLength(env, iv);
	pbyte = (*env)->GetByteArrayElements(env, iv, 0);

	g_iv = (unsigned char*)malloc(n);
	memcpy(g_iv, pbyte, n);
	for(i=0;i<n;i++) {
		v = g_key[i];
		(*env)->GetByteArrayRegion(env, iv, i, 1, &value);
		LOGE("iv[%d]=%d(%d)\r\n",i, value, v);
	}

	nRet = n;



#if 0
        ENGINE_load_builtin_engines();

        e = ENGINE_get_first();
        while( e ) {
                LOGE("%s [%s]\n", ENGINE_get_name(e), ENGINE_get_id(e));
                e = ENGINE_get_next(e);
        }
        ENGINE_cleanup();
#endif


	LOGD("Initializing AES ALGORITHM FOR CBC MODE..\n");

	ENGINE_load_cryptodev();

	if (!(e = ENGINE_by_id("cryptodev")))
		LOGE("err: engine_by_id,ret=%lx\n", ERR_get_error());
	if (!ENGINE_set_default(e, ENGINE_METHOD_ALL))
		LOGE("err: engine_set_default fail on eng=%s\n", (char*)e);

	if (!ENGINE_init(e))
		LOGE("err: engine init ret=%lu\n", ERR_get_error());


	read_buf = malloc(BUFSIZE);

	cipher_type = EVP_aes_128_cbc();

	EVP_CIPHER_CTX_init(&en);
	EVP_CIPHER_CTX_init(&de);

   	EVP_EncryptInit_ex(&en, cipher_type, e, g_key, g_iv);
   	EVP_DecryptInit_ex(&de, cipher_type, e, g_key, g_iv);


	return nRet;
}

/*
 * Class:     com_example_progressbar_CryptoDev
 * Method:    en_crypt
 * Signature: (Ljava/io/FileDescriptor;Ljava/io/FileDescriptor;)I
 */
JNIEXPORT jint JNICALL Java_com_example_progressbar_CryptoDev_en_1crypt
  (JNIEnv *env, jobject thiz, jstring input, jstring output)
{
	jint nRet = 0;
	jboolean iscopy;
	FILE *fIN, *fOUT;
	jmethodID mid = NULL;
	jclass cls = (*env)->GetObjectClass(env, thiz);

	if( mid == NULL) {
		mid = (*env)->GetMethodID(env, cls, "onEventCipher", "(I)V");
		if( mid == NULL ) {
			return -2;
		}
	}


	const char *in_path_utf = (*env)->GetStringUTFChars(env, input, &iscopy);
	LOGD("Opening output=%s ", in_path_utf);
	fIN = fopen(in_path_utf, "rb");
	(*env)->ReleaseStringUTFChars(env, input, in_path_utf);
	if (fIN == NULL)
	{
		/* Throw an exception */
		LOGE("Cannot open input file");
		/* TODO: throw an exception */
		return -1;
	}

	const char *out_path_utf = (*env)->GetStringUTFChars(env, output, &iscopy);
	LOGD("Opening output=%s ", out_path_utf);
	fOUT = fopen(out_path_utf, "wb");
	(*env)->ReleaseStringUTFChars(env, input, in_path_utf);
	if (fOUT == NULL)
	{
		/* Throw an exception */
		LOGE("Cannot open output file");
		/* TODO: throw an exception */
		return -1;
	}


	// First encrypt the file
//	fIN = fopen("/data/local/tmp/plain.txt", "rb"); //File to be encrypted; plain text
//	fOUT = fopen("/data/local/tmp/ciphertext.txt", "wb"); //File to be written; cipher text

	en_crypt( fIN, fOUT, g_key, g_iv, env, thiz, mid);

	fclose(fIN);
	fclose(fOUT);

	LOGD("encryption complete!!!\n");

	return nRet;
}

/*
 * Class:     com_example_progressbar_CryptoDev
 * Method:    de_crypt
 * Signature: (Ljava/io/FileDescriptor;Ljava/io/FileDescriptor;)I
 */
JNIEXPORT jint JNICALL Java_com_example_progressbar_CryptoDev_de_1crypt
  (JNIEnv *env, jobject thiz, jstring input, jstring output)
{
	jint nRet = 0;
	jboolean iscopy;
	FILE *fIN, *fOUT;
	jmethodID mid = NULL;
	jclass cls = (*env)->GetObjectClass(env, thiz);

	if( mid == NULL) {
		mid = (*env)->GetMethodID(env, cls, "onEventCipher", "(I)V");
		if( mid == NULL ) {
			return -2;
		}
	}


	const char *in_path_utf = (*env)->GetStringUTFChars(env, input, &iscopy);
	LOGD("Opening output=%s ", in_path_utf);
	fIN = fopen(in_path_utf, "rb");
	(*env)->ReleaseStringUTFChars(env, input, in_path_utf);
	if (fIN == NULL)
	{
		/* Throw an exception */
		LOGE("Cannot open input file");
		/* TODO: throw an exception */
		return -1;
	}

	const char *out_path_utf = (*env)->GetStringUTFChars(env, output, &iscopy);
	LOGD("Opening output=%s ", out_path_utf);
	fOUT = fopen(out_path_utf, "wb");
	(*env)->ReleaseStringUTFChars(env, input, in_path_utf);
	if (fOUT == NULL)
	{
		/* Throw an exception */
		LOGE("Cannot open output file");
		/* TODO: throw an exception */
		return -1;
	}

	de_crypt( fIN, fOUT, g_key, g_iv, env, thiz, mid);

	fclose(fIN);
	fclose(fOUT);

	LOGD("decryption complete!!!\n");

	return nRet;
}

/*
 * Class:     com_example_progressbar_CryptoDev
 * Method:    engine_close
 * Signature: ()I
 */
JNIEXPORT void JNICALL Java_com_example_progressbar_CryptoDev_engine_1close
  (JNIEnv *env, jobject thiz)
{
	if( read_buf != NULL )
		free(read_buf);

	EVP_CIPHER_CTX_cleanup(&de);
	EVP_CIPHER_CTX_cleanup(&en);

	ENGINE_finish(e);
	ENGINE_free(e);

	if( g_key != NULL) {
		free(g_key);
	}
	if( g_iv != NULL) {
		free(g_iv);
	}

	LOGD("freeing all buffer !!!\n");
}
