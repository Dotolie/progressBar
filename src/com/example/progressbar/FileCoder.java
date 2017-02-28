package com.example.progressbar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class FileCoder {
	private Key key;
	private IvParameterSpec iv;
	private boolean bRun = true;
	private Handler mHandler = null;
	private static final String TAG = "FileCoder";
    private static final String algorithm = "AES";
	private static final String transformation = algorithm + "/CBC/PKCS5Padding";


	public FileCoder(String strKey, String strIv, Handler handle) {
		
		Key pkey = new SecretKeySpec(strKey.getBytes(), 0 , 16, "AES");
		IvParameterSpec iv = new IvParameterSpec(strIv.getBytes(), 0,16);

		this.key = pkey;
		this.iv = iv;
		this.mHandler = handle;
	}

	public void abort() {
		bRun = false;
	}
	
	public void encrypt(File source, File outf) throws Exception {
		crypt(Cipher.ENCRYPT_MODE, source, outf);
	}
	

	public void decrypt(File source, File outf) throws Exception {
		crypt(Cipher.DECRYPT_MODE, source, outf);
	}

	private void crypt(int mode, File source, File target) throws Exception {

		Cipher cipher = Cipher.getInstance(transformation);
		
		try {
			cipher.init(mode, key, iv);
		}
		catch( Exception e) {
			Log.e(TAG, "Exception="+e);
		}
		
		InputStream input = null;
		OutputStream output = null;
		bRun = true;
		
		try {
			input = new BufferedInputStream(new FileInputStream(source));
			output = new BufferedOutputStream( new FileOutputStream(target));
			byte[] buffer = new byte[8192];
			int read = -1;
			
			while ((read = input.read(buffer)) != -1) {
				byte[] vbuffer = cipher.update(buffer, 0, read);
				output.write(vbuffer);
				Message msg = mHandler.obtainMessage();
				msg.what = 2;
				msg.arg1 = read;
				mHandler.sendMessage(msg);
				if( bRun == false) {
					break;
				}
			}

			byte[] vbuffer =  cipher.doFinal();
			output.write(vbuffer);
			output.flush();
			Message msg = mHandler.obtainMessage();
			msg.what = 2;
			msg.arg1 = read;
			mHandler.sendMessage(msg);


		} finally {
			if (input != null) {
				try { 
					input.close();
				} catch(IOException ie) {
					Log.e(TAG, "exception:input close");
				}			
			}
			if (output != null) {
				try { 
					output.close();
				} catch(IOException ie) {
					Log.e(TAG,  "exception:output close");
				}			
			}
		}
	}
}
