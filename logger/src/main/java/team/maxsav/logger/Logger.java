package team.maxsav.logger;

import android.content.Context;
import android.os.Process;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.CallSuper;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author Max Savitsky
 * */
public class Logger {

	private static String mRsaPublicKey;
	private static boolean isDebug = false;
	private static boolean initialized = false;
	private final ArrayList<String> mBuffer = new ArrayList<>();
	private static final int BUFFER_SIZE = 10;
	private final Writer mWriter;

	/**
	 * RSA public key.
	 * You can use it to protect log from users, but it can be decrypted by anyone who saw this file,
	 * because of {@link Logger#DEFAULT_PRIVATE_KEY} in this file
	 * */
	public static final String DEFAULT_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQosywLGiHrN1ZtNK5dz37W7/ty/nI43XEAh6uOKp6Vw7Gy8n4Tqxm4e8lS1uyxpH03BSr9yHDPz4qMOERVMdBLL0GjxL5jDKS4FGLCw3AYSP5B34ImlFhbJo/EPrrCid1vtFHIy0vPBsOvuSSiMLjPejLekj7yhoRMSD+eNEvEUgQIDAQAB";

	/**
	 * RSA private key.
	 * You can use it to decrypt log, which was encrypted with {@link Logger#DEFAULT_PUBLIC_KEY}
	 * */
	public static final String DEFAULT_PRIVATE_KEY = "MIICeQIBADANBgkqhkiG9w0BAQEFAASCAmMwggJfAgEAAoGBCizLAsaIes3Vm00rl3Pftbv+3L+cjjdcQCHq44qnpXDsbLyfhOrGbh7yVLW7LGkfTcFKv3IcM/Piow4RFUx0EsvQaPEvmMMpLgUYsLDcBhI/kHfgiaUWFsmj8Q+usKJ3W+0UcjLS88Gw6+5JKIwuM96Mt6SPvKGhExIP540S8RSBAgMBAAECgYEHjUFLmRB4nMa6UidVbEIQfyxkqK5Ie1wzmTjdku5kgxBmkASRQTLvTnarWopGJut96UVSHB5EjPGb9XfGaA0KVYhUJlN9GiN8cB4xniLt6koORihEtdZ0kAquvWWqPAFZqRsQEAqivcvcHzRFal6oxf2NPYTrZeRcftYKY3j+j0UCQQOYLnMJaqmjpsftqRXVjGOeeuEYGQhb05p7VyBKtcfFf71zPsWwWg9Z3bP0VcX03QVcxB8nEzYDUpMMJGMdUF+HAkEC1KtGx0uqCV1Y3pR9i0hoh+R1Ef3gjpEFqH1C+I1uk//TNOxb2lcAQZ/3dPuJ3JZ4UFwiZ57CKGZRaa4dQKGdtwJBAoUOGsqBIUZ9xi2OmKXI8pTIYz83XSqyHdtU6mg1IkQLFk3RtVe46oX+6wXfkxPiVL4BJi2IRBb0Le0XHPwRucUCQQCoXv878PWZ5WlvlbqxsOowoMEepAkXttREuI3l6B6IHol5I22YBlzV4pABSyxV51Qe/7kysC1Wa6eA0WaUjLhzAkEBa3mBT41uiNlLfEo39Pf6nGNifbKnzZdqs9qz58MjQ4xepKwPQ1Fkz2isEyODobGrLFka6BhNaH/xRp1N2eDPyg==";

	private static Logger instance;

	private static Logger getInstance() {
		return instance;
	}

	/**
	 * Initializes logger. Should be called before usage
	 *
	 * @param context application context
	 * @param rsaPublicKey RSA public key to encrypt logs. If yo do not want this, pass {@code null}.
	 *                     You can use default public ({@link Logger#DEFAULT_PUBLIC_KEY})
	 *                     and private ({@link Logger#DEFAULT_PRIVATE_KEY}) keys
	 * @param debug If {@code true} logs also will be written in system log
	 * @throws IOException This exception will be thrown if writer couldn't create file for current session
	 * */
	public static void initialize(Context context, String rsaPublicKey, boolean debug) throws IOException {
		isDebug = debug;
		if ( rsaPublicKey != null ) {
			if ( verifyKey( rsaPublicKey ) ) {
				mRsaPublicKey = rsaPublicKey;
			} else {
				throw new IllegalArgumentException( "invalid rsa key" );
			}
		}
		mRsaPublicKey = rsaPublicKey;
		initialized = true;
		instance = new Logger( context );
	}

	private static boolean verifyKey(String stringKey) {
		X509EncodedKeySpec spec = new X509EncodedKeySpec( Base64.decode( stringKey, Base64.DEFAULT ) );
		try {
			PublicKey key = KeyFactory.getInstance( "RSA" ).generatePublic( spec );
			return true; // if no exceptions
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private Logger(Context context) throws IOException {
		mWriter = new Writer( context, mRsaPublicKey );
	}

	/**
	 * Function returns path for current log file
	 * */
	public static String getPathForLog(){
		return getInstance().mWriter.getPath();
	}

	/**
	 * Call this function when your app is destroying.
	 * After function call, all messages in buffer will be added to writer and цшдд иу written into log
	 * */
	public static void closeLogger(){
		getInstance().close();
	}

	private void close(){
		synchronized (mBuffer) {
			mWriter.addAll( mBuffer );
			mBuffer.clear();
			mWriter.close();
		}
		initialized = false;
	}

	@Override
	protected void finalize() throws Throwable {
		if(initialized){
			close();
		}
		super.finalize();
	}

	private static void checkIfInitialized() {
		if ( !initialized ) {
			throw new IllegalStateException( "Logger not initialized. Please call initialize function before use" );
		}
	}

	public static void i(String tag, String message){info(tag, message );}
	public static void info(String tag, String message) {
		if ( isDebug ) {
			Log.i( tag, message );
		}
		checkIfInitialized();
		getInstance().addRecord( "I", tag, message );
	}

	public static void e(String tag, String message){error(tag, message );}
	public static void error(String tag, String message) {
		if ( isDebug ) {
			Log.e( tag, message );
		}
		checkIfInitialized();
		getInstance().addRecord( "E", tag, message );
	}

	public static void v(String tag, String message){verbose(tag, message );}
	public static void verbose(String tag, String message) {
		if ( isDebug ) {
			Log.v( tag, message );
		}
		checkIfInitialized();
		getInstance().addRecord( "V", tag, message );
	}

	public static void d(String tag, String message){debug(tag, message );}
	public static void debug(String tag, String message) {
		if ( isDebug ) {
			Log.d( tag, message );
		}
		checkIfInitialized();
		getInstance().addRecord( "D", tag, message );
	}

	public static void w(String tag, String message){warn(tag, message );}
	public static void warn(String tag, String message) {
		if ( isDebug ) {
			Log.w( tag, message );
		}
		checkIfInitialized();
		getInstance().addRecord( "W", tag, message );
	}

	private void addRecord(String level, String tag, String message) {
		String time = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS" ).format( new Date() );
		String out = level + ":" + time + " " + Process.myUid() + " " + Thread.currentThread().getId() + "/" + tag + ": " + message;
		synchronized (mBuffer) {
			mBuffer.add( out );
			if ( mBuffer.size() >= BUFFER_SIZE ) {
				mWriter.addAll( mBuffer );
				mBuffer.clear();
			}
		}
	}

}
