package team.maxsav.logger;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.crypto.Cipher;

class Writer {
	private final FileWriter mFileWriter;
	private final Queue<String> mQueue = new ConcurrentLinkedQueue<>();
	private boolean isWriting = false;
	private final String rsaPublicKey;

	private final Thread mThread = new Thread(()->{
		while ( !Thread.currentThread().isInterrupted() ) {
			if ( !isWriting ) {
				write();
			}
		}
	});

	private final String path;

	Writer(Context context, String rsaPublicKey) throws IOException {
		this.rsaPublicKey = rsaPublicKey;

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
		mQueue.add( "" );
		mThread.start();
	}

	public String getPath() {
		return path;
	}

	public void addAll(ArrayList<String> arrayList){
		mQueue.addAll( arrayList );
	}

	private boolean needToClose = false;

	public void close(){
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
		if (rsaPublicKey  == null || text.equals( "" ) ) {
			return text;
		}

		X509EncodedKeySpec spec = new X509EncodedKeySpec( Base64.decode( rsaPublicKey, Base64.DEFAULT ) );
		try {
			Key publicKey = KeyFactory.getInstance( "RSA" ).generatePublic( spec );
			Cipher cipher = Cipher.getInstance( "RSA" );
			cipher.init( Cipher.ENCRYPT_MODE, publicKey );
			byte[] bytes = cipher.doFinal( text.getBytes() );
			StringBuilder sb = new StringBuilder();
			for (byte b : bytes) {
				sb.append( String.format( "%02x", b ) );
			}
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return text;
		}
	}

}
