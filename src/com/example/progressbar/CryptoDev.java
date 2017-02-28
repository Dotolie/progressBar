package com.example.progressbar;

import android.util.Log;

public class CryptoDev {
	private static final String TAG = "CryptoDev";
	/*
	 * Do not remove or rename the field mFd: it is used by native method close();
	 */
	static {
		try {	
			Log.e(TAG, "System.loadLibrary");
			System.loadLibrary("cryptodev");
		} catch (UnsatisfiedLinkError e) {
			// only ignore exception in non-android env
			if ("Dalvik".equals(System.getProperty("java.vm.name"))) throw e;
		}
	}
	
	public interface EventListner {
		void onCiphered(int nSize);
	}
	private static EventListner mCipherCb;
	
	public void setOnEventListner( EventListner callback) {
		mCipherCb = callback;
	}

	// JNI
	public native int engine_init(byte[] key, byte[] iv);
	public native int en_crypt(String fi, String fo);
	public native int de_crypt(String fi, String fo);
	public native void engine_close();
	public void onEventCipher(int nSize) {
//		Log.e(TAG, "nSize="+nSize);
		mCipherCb.onCiphered(nSize);
	}
}
