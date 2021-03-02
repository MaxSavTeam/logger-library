package team.maxsav.logger;

import android.util.Base64;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;

import javax.crypto.Cipher;

/**
 * Class indented to decrypt encrypted log files with specified RSA private key
 *
 * @author Max Savitsky
 * */
public class LogDecryptor {

	private Cipher mCipher;
	private int blockSize;

	public LogDecryptor(String privateKeyString){
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec( Base64.decode( privateKeyString, 0 ) );
		try {
			PrivateKey privateKey = KeyFactory.getInstance( "RSA" ).generatePrivate( spec );
			mCipher = Cipher.getInstance( "RSA" );
			mCipher.init( Cipher.DECRYPT_MODE, privateKey );
			blockSize = mCipher.getBlockSize();
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
	}

	public static ArrayList<String> readFileLinesWithoutPartSeparator(String path) throws IOException {
		ArrayList<String> strings = new ArrayList<>();
		BufferedReader reader = new BufferedReader( new FileReader( new File( path ) ) );
		while(reader.ready()){
			String line = reader.readLine();
			if(!line.equals( Writer.partsSeparatorString ))
				strings.add( line );
		}
		return strings;
	}


	/**
	 * Return list of decrypted log message with additional info at the beginning of log
	 *
	 * @param pathToLog path to log file. Can be obtained from {@link Logger#getPathForLog()}
	 *
	 * @throws IOException if io error occurred during file opening
	 * @throws GeneralSecurityException if there are some errors during log decryption
	 * */
	public ArrayList<String> decryptLogFile(String pathToLog) throws IOException, GeneralSecurityException {
		ArrayList<String> messages = readFileLinesWithoutPartSeparator( pathToLog );
		ArrayList<String> decrypted = new ArrayList<>();
		for(String line : messages){
			if(!line.equals( Writer.partsSeparatorString )){
				decrypted.add( decrypt( line ) );
			}
		}
		return decrypted;
	}

	/**
	 * Returns decrypted message from log
	 *
	 * @param s encrypted message
	 *
	 * @throws GeneralSecurityException if there is some error during decryption
	 * */
	public String decrypt(String s) throws GeneralSecurityException {
		if(s.length() > blockSize){
			String[] strings = s.split( " " );
			String result = "";
			for(String string : strings){
				result = String.format( "%s%s", result, decryptPart( string ) );
			}
			return result;
		}
		return decryptPart( s );
	}

	private String decryptPart(String s) throws GeneralSecurityException{
		byte[] decodedBytes = new byte[s.length() / 2];
		for(int i = 0; i < s.length(); i += 2){
			decodedBytes[i / 2] = (byte) Integer.parseInt( s.substring( i, i + 2 ), 16 );
		}
		byte[] bytes = mCipher.doFinal(decodedBytes);
		return new String(bytes);
	}

}
