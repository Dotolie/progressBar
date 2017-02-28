package com.example.progressbar;

import java.io.File;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	private final String TAG = "AES demo";
	
	public static final int SEND_PROGRESS_START = 1;
    public static final int SEND_PROGRESS_ING = 2;
    public static final int SEND_PROGRESS_END = 3;
	public static final int SEND_PROGRESS_START_CRYPTO = 4;
    public static final int SEND_PROGRESS_ING_CRYPTO = 5;
    public static final int SEND_PROGRESS_END_CRYPTO = 6;
    
    private static final int FILE_SELECT_CODE = 0;

	public Button mBtFile;
	public Button mBtEncrypt;
	public Button mBtDecrypt;
	public Button mBtEncryptCrypto;
	public Button mBtDecryptCrypto;
	public TextView mTvPlainFile;
	public TextView mTvElapseTime;
	public TextView mTvFileSize;
	public TextView mTvElapseTimeCrypto;
	public TextView mTvFileSizeCrypto;
	
	public FileCoder mFCoder;
	
	public SendMsgHandler mMainHandler = null;
	public String dirPath;
	public String filePath;
	public int nFileSize = 0;
	public int nCurrentSize = 0;
	public long lStartTime = 0;
	public long lElapsedTime = 0;
	
	private EncryptThread mEncryptThread = null;
	private DecryptThread mDecryptThread = null;
	private EncryptThreadCrypto mEncryptThreadCrypto = null;
	private DecryptThreadCrypto mDecryptThreadCrypto = null;
	
	public ProgressDialog mDialog;
	public CryptoDev mCryptoDev;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mMainHandler = new SendMsgHandler();
		mFCoder = new FileCoder("thiskeyisverybad", "dontusethisinput", mMainHandler);
		
		mCryptoDev = new CryptoDev();
		mCryptoDev.engine_init("thiskeyisverybad".getBytes(), "dontusethisinput".getBytes());
		dirPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		
		CryptoDev.EventListner callback = new CryptoDev.EventListner() {
			
			@Override
			public void onCiphered(int nSize) {
				// TODO Auto-generated method stub
				Message msg = mMainHandler.obtainMessage();
				msg.what = SEND_PROGRESS_ING_CRYPTO;
				msg.arg1 = nSize;
				mMainHandler.sendMessage(msg);

			}
		};
		
		mCryptoDev.setOnEventListner(callback);
//		try {
//			File file = new File(dirPath);
//			
//			if(!file.exists())
//				file.mkdirs();
//		}
//		catch( Exception e) {
//			Log.e(TAG, "can't make dir");
//		}
		
		SetupViews();	
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	void SetupViews() {
		mBtFile = (Button)findViewById(R.id.btFile);
		mBtFile.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.setType("*/*");
				Intent i = Intent.createChooser(intent, "Open file");
				startActivityForResult(intent, FILE_SELECT_CODE);								
			}
		});

		mBtEncrypt = (Button)findViewById(R.id.btEncrypt);
		mBtEncrypt.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mEncryptThread = new EncryptThread();
				mEncryptThread.start();				
			}
			
		});
		mBtDecrypt = (Button)findViewById(R.id.btDecrypt);
		mBtDecrypt.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mDecryptThread = new DecryptThread();
				mDecryptThread.start();
			}
		});
		mBtEncryptCrypto = (Button)findViewById(R.id.btEncryptCrypto);
		mBtEncryptCrypto.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mEncryptThreadCrypto = new EncryptThreadCrypto();
				mEncryptThreadCrypto.start();				
			}
			
		});
		mBtDecryptCrypto = (Button)findViewById(R.id.btDecryptCrypto);
		mBtDecryptCrypto.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mDecryptThreadCrypto = new DecryptThreadCrypto();
				mDecryptThreadCrypto.start();
			}
		});

		
		mTvPlainFile = (TextView)findViewById(R.id.tvPlainFile);
		mTvFileSize = (TextView)findViewById(R.id.tvFileSize);
		mTvElapseTime = (TextView)findViewById(R.id.tvElapseTime);
		mTvFileSizeCrypto = (TextView)findViewById(R.id.tvFileSizeCrypto);
		mTvElapseTimeCrypto = (TextView)findViewById(R.id.tvElapseTimeCrypto);
	}
	
	private class EncryptThread extends Thread {		
		@Override
		public void run() {
			super.run();
			
			Message msg = mMainHandler.obtainMessage();
			msg.what = SEND_PROGRESS_START;
			mMainHandler.sendMessage(msg);

			try {	
				File ifile = new File(dirPath + "/"+ filePath);
				File ofile = new File(dirPath + "/"+ filePath+".enc");
				
				nFileSize = (int)ifile.length();			
				
				mFCoder.encrypt(ifile, ofile);
			}
			catch( Exception e ) {
				Log.e(TAG, "exception: encrypt="+e);
			}	

			msg = mMainHandler.obtainMessage();
			msg.what = SEND_PROGRESS_END;
			mMainHandler.sendMessage(msg);
		}
	}

	private class DecryptThread extends Thread {		
		@Override
		public void run() {
			super.run();

			Message msg = mMainHandler.obtainMessage();
			msg.what = SEND_PROGRESS_START;
			mMainHandler.sendMessage(msg);
			
			try {	
				File ifile = new File(dirPath + "/" + filePath+".enc");
				File ofile = new File(dirPath + "/" + filePath+".dec");
				
				nFileSize = (int)ifile.length();
				
				mFCoder.decrypt(ifile, ofile);
			}
			catch( Exception e ) {
				Log.e(TAG, "exception: decrypt="+e);
			}
			
			msg = mMainHandler.obtainMessage();
			msg.what = SEND_PROGRESS_END;
			mMainHandler.sendMessage(msg);
			
		}
	}
	private class EncryptThreadCrypto extends Thread {		
		@Override
		public void run() {
			super.run();
			
			Message msg = mMainHandler.obtainMessage();
			msg.what = SEND_PROGRESS_START_CRYPTO;
			mMainHandler.sendMessage(msg);

			try {	
				File ifile = new File(dirPath + "/"+ filePath);
				nFileSize = (int)ifile.length();			
				ifile = null;
				
				String input = dirPath + "/" + filePath;
				String output = dirPath + "/" + filePath+".enc";
				mCryptoDev.en_crypt(input, output);
			}
			catch( Exception e ) {
				Log.e(TAG, "exception: encrypt_crypto="+e);
			}	

			msg = mMainHandler.obtainMessage();
			msg.what = SEND_PROGRESS_END_CRYPTO;
			mMainHandler.sendMessage(msg);
		}
	}

	private class DecryptThreadCrypto extends Thread {		
		@Override
		public void run() {
			super.run();

			Message msg = mMainHandler.obtainMessage();
			msg.what = SEND_PROGRESS_START_CRYPTO;
			mMainHandler.sendMessage(msg);
			
			try {	
				File ifile = new File(dirPath + "/" + filePath+".enc");
				nFileSize = (int)ifile.length();				
				ifile = null;
				
				String input = dirPath + "/" + filePath + ".enc";
				String output = dirPath + "/" + filePath+".dec";
				mCryptoDev.de_crypt(input, output);
				
			}
			catch( Exception e ) {
				Log.e(TAG, "exception: decrypt_crypto="+e);
			}
			
			msg = mMainHandler.obtainMessage();
			msg.what = SEND_PROGRESS_END_CRYPTO;
			mMainHandler.sendMessage(msg);
			
		}
	}
	
	class SendMsgHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			switch (msg.what) {
			case SEND_PROGRESS_START:
			case SEND_PROGRESS_START_CRYPTO:
				nCurrentSize = 0;
//				mProgress.setProgress(0);
				lStartTime = System.currentTimeMillis();

				showDialog(0);
				mDialog.setProgress(0);
				break;
			case SEND_PROGRESS_ING:
				nCurrentSize += msg.arg1;
//				mProgress.incrementProgressBy (msg.arg1);
				lElapsedTime = (System.currentTimeMillis()-lStartTime)/1000;
//				mTvElapsed.setText(lElapsedTime + " sec");
				float nProg = nCurrentSize*100f/nFileSize;
				mDialog.setMessage(lElapsedTime + " sec");
				mDialog.setProgress((int)nProg);
				break;
			case SEND_PROGRESS_ING_CRYPTO:
				nCurrentSize += msg.arg1;
//				mProgress.incrementProgressBy (msg.arg1);
				lElapsedTime = (System.currentTimeMillis()-lStartTime)/1000;
//				mTvElapsed.setText(lElapsedTime + " sec");
				float nProgCrypto = nCurrentSize*100f/nFileSize;
				mDialog.setMessage(lElapsedTime + " sec");
				mDialog.setProgress((int)nProgCrypto);
				break;

			case SEND_PROGRESS_END:
				mDialog.dismiss();
				lElapsedTime = (System.currentTimeMillis()-lStartTime)/1000;
				mTvFileSize.setText(nFileSize+" B");
				mTvElapseTime.setText(lElapsedTime+" sec");
				break;

			case SEND_PROGRESS_END_CRYPTO:
				mDialog.dismiss();
				lElapsedTime = (System.currentTimeMillis()-lStartTime)/1000;
				mTvFileSizeCrypto.setText(nFileSize+" B");
				mTvElapseTimeCrypto.setText(lElapsedTime+" sec");
				break;
				
			default:
				break;
			}
		}
	}
	
	public String getFileName(Uri uri) {
		String result = null;
		if (uri.getScheme().equals("content")) {
			Cursor cursor = getContentResolver().query(uri, null, null, null, null);
			try {
				if (cursor != null && cursor.moveToFirst()) {
//					result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
					String name=cursor.getColumnName(0);
					result = cursor.getString(cursor.getColumnIndex(name));
				}
			} finally {
				cursor.close();
			}
		}
		if (result == null) {
			result = uri.getPath();
			int cut = result.lastIndexOf('/');
			if (cut != -1) {
				result = result.substring(cut + 1);
			}
		}
		return result;
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    switch( requestCode ) {
	    case FILE_SELECT_CODE:
	    	if( resultCode == RESULT_OK ) {
	    		Uri uri = data.getData();
	    		String PathName=getFileName(uri);
	    		String[] subName = PathName.split(":");
	    		int cut = subName[1].lastIndexOf("/");
	    		String path = subName[1].substring(0,cut);
	    		String file = subName[1].substring(cut+1);
	    		filePath = subName[1];
	    		mTvPlainFile.setText(filePath);
	    		Log.d(TAG, "filePath=" + filePath);
	    	}
	    	break;
	    }
	    super.onActivityResult(requestCode, resultCode, data);
	}
	
	protected Dialog onCreateDialog( int id) {
		switch(id) {
		case 0:
			mDialog = new ProgressDialog(this);
			mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mDialog.setTitle("En/Decrypting");
			mDialog.setMessage("Wait...");
			mDialog.setCancelable(false);
			
			mDialog.setButton("Cancel", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					mFCoder.abort();
					dismissDialog(0);
				}
			});

			return mDialog;
		}
		return null;
	}
}
