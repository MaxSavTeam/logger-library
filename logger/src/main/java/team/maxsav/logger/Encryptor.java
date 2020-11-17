package team.maxsav.logger;

import android.util.Base64;
import android.util.Log;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

import javax.crypto.Cipher;

class Encryptor {

	private Cipher mCipher;
	private int blockSize;

	Encryptor(String key) {
		X509EncodedKeySpec spec = new X509EncodedKeySpec( Base64.decode( key, Base64.DEFAULT ) );
		try {
			Key publicKey = KeyFactory.getInstance( "RSA" ).generatePublic( spec );
			mCipher = Cipher.getInstance( "RSA" );
			mCipher.init( Cipher.ENCRYPT_MODE, publicKey );
			blockSize = mCipher.getBlockSize();
		} catch (GeneralSecurityException e) {
			// never will be thrown, because key verified during logger initialization
			e.printStackTrace();
		}
	}

	private String encryptPart(String text) throws GeneralSecurityException {
		byte[] bytes = mCipher.doFinal(text.getBytes());
		String out = "";
		for (byte b : bytes) {
			out = String.format( "%s%02x", out, b );
		}
		return out;
	}

	String encrypt(String text) throws GeneralSecurityException {
		if(text.length() <= blockSize){
			return encryptPart( text );
		}else{
			ArrayList<String> parts = new ArrayList<>();
			for(int i = 0; i < text.length(); i += blockSize){
				if(i + blockSize < text.length()){
					parts.add( text.substring( i, i + blockSize ) );
				}else{
					parts.add( text.substring( i ) );
				}
			}
			StringBuilder resultString = new StringBuilder();
			for(int i = 0; i < parts.size(); i++){
				resultString.append( encryptPart( parts.get( i ) ) );
				if(i != parts.size() - 1)
					resultString.append( " " );
			}
			LogDecryptor logDecryptor = new LogDecryptor(Logger.DEFAULT_PRIVATE_KEY);
			String result = resultString.toString();
			Log.i( "Logger Encryptor", "encrypt: test passed = " + text.equals( logDecryptor.decrypt( result ) )  );
			return resultString.toString();
		}
	}

}
