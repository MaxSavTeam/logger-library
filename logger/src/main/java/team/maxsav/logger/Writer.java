package team.maxsav.logger;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

class Writer {
	private final FileWriter mFileWriter;
	private final Queue<String> mQueue = new ConcurrentLinkedQueue<>();
	private boolean isWriting = false;
	private final String rsaPublicKey;
	private Encryptor mEncryptor;

	private final Thread mThread = new Thread(()->{
		while ( !Thread.currentThread().isInterrupted() ) {
			if ( !isWriting ) {
				write();
			}
		}
	});

	private final String path;

	public static final String partsSeparatorString = "================";
	public int additionalInfoLinesCount;

	Writer(Context context, String rsaPublicKey) throws IOException {
		this.rsaPublicKey = rsaPublicKey;
		if(rsaPublicKey != null)
			mEncryptor = new Encryptor( rsaPublicKey );

		String time = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format( new Date() );
		String externalFilesDir = context.getApplicationContext().getExternalFilesDir( null ).getPath();
		File file = new File( externalFilesDir + "/logger" );
		if(!file.exists())
			file.mkdir();
		file = new File( file.getPath() + "/log_" + time + ".logx" );
		file.createNewFile();
		path = file.getPath();
		mFileWriter = new FileWriter( file );
		mQueue.add( time );
		mQueue.add( "Brand: " + Build.BRAND );
		mQueue.add( "Manufacturer: " + Build.MANUFACTURER );
		mQueue.add( "Device: " + Build.DEVICE );
		mQueue.add( "Model: " + Build.MODEL );
		mQueue.add( "Hardware: " + Build.HARDWARE );
		mQueue.add( "Android SDK version: " + Build.VERSION.SDK_INT );
		mQueue.add( "Application package: " + context.getPackageName() );
		try {
			PackageInfo pInfo = context.getPackageManager().getPackageInfo( context.getPackageName(), 0 );
			mQueue.add( "Version: " + pInfo.versionName );
			mQueue.add( "Version code: " + pInfo.versionCode );
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		additionalInfoLinesCount = mQueue.size();
		mQueue.add( partsSeparatorString );
		mThread.start();
	}

	String getPath() {
		return path;
	}

	int getAdditionalInfoLinesCount() {
		return additionalInfoLinesCount;
	}

	void addAll(ArrayList<String> arrayList){
		mQueue.addAll( arrayList );
	}

	private boolean needToClose = false;

	void close(){
		mThread.interrupt();
		needToClose = true;
	}

	private void write(){
		if(mQueue.size() > 0){
			String element = mQueue.poll();
			element = encrypt( element );
			try {
				mFileWriter.write( element + "\n" );
				mFileWriter.flush();
				if(needToClose){
					mFileWriter.close();
					isWriting = false;
					return;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			write();
		}else{
			isWriting = false;
		}
	}

	private String encrypt(String text) {
		if (rsaPublicKey  == null || text.equals( partsSeparatorString ) ) {
			return text;
		}

		try {
			return mEncryptor.encrypt( text );
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
			Log.i( "Logger Writer", "encrypt: " + e );
			return text;
		}
	}

}
