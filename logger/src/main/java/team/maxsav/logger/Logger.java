package team.maxsav.logger;

import android.content.Context;
import android.os.Process;
import android.util.Base64;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Max Savitsky
 */
public class Logger {

	private static String mRsaPublicKey;
	private static boolean isDebug = false;
	private static boolean initialized = false;
	private final ArrayList<String> mBuffer = new ArrayList<>();
	private static final int BUFFER_SIZE = 10;
	private final Writer mWriter;
	private Timer mTimer;

	public static class Initializer{
		private final Context mContext;
		private String rsaPublicKey = DEFAULT_PUBLIC_KEY;
		private boolean isDebug = false;
		private int timerPeriod = 20;
		private boolean autoFlushOnException = true;
		private boolean printErrorOnException = true;

		public Initializer(@NotNull Context context) {
			mContext = context.getApplicationContext();
		}

		/**
		 * Sets RSA key to encrypt log messages (pass null to disable).
		 * Default is {@link Logger#DEFAULT_PUBLIC_KEY}
		 * */
		public Initializer setRsaPublicKey(String rsaPublicKey) {
			this.rsaPublicKey = rsaPublicKey;
			return this;
		}

		/**
		 * Sets flag to send logs to default Android log
		 * */
		public Initializer setDebug(boolean debug) {
			isDebug = debug;
			return this;
		}

		/**
		 * Sets the timer period (in seconds) after which all messages from the buffer are automatically written to the file.
		 * Pass 0 to disable timer.
		 * Default is 20
		 * */
		public Initializer setTimerPeriod(int timerPeriod) {
			this.timerPeriod = timerPeriod;
			return this;
		}

		/**
		 * Sets flag to flush on uncaught exception.
		 * If you want to use auto flush and your own uncaught exception handler, please initialize Logger AFTER you set your own handler,
		 * because logger overrides exception handler, while retaining the previous one
		 * */
		public Initializer setAutoFlushOnException(boolean autoFlushOnException) {
			this.autoFlushOnException = autoFlushOnException;
			return this;
		}

		/**
		 * Sets flag to print uncaught exception to log file.
		 * If you want to use auto flush and your own uncaught exception handler, please initialize Logger AFTER you set your own handler,
		 * because logger overrides exception handler, while retaining the previous one
		 * */
		public Initializer setPrintErrorOnException(boolean printErrorOnException) {
			this.printErrorOnException = printErrorOnException;
			return this;
		}
	}

	/**
	 * RSA public key.
	 * You can use it to protect log from users, but it can be decrypted by anyone who saw this file,
	 * because of {@link Logger#DEFAULT_PRIVATE_KEY} in this file
	 */
	public static final String DEFAULT_PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQosywLGiHrN1ZtNK5dz37W7/ty/nI43XEAh6uOKp6Vw7Gy8n4Tqxm4e8lS1uyxpH03BSr9yHDPz4qMOERVMdBLL0GjxL5jDKS4FGLCw3AYSP5B34ImlFhbJo/EPrrCid1vtFHIy0vPBsOvuSSiMLjPejLekj7yhoRMSD+eNEvEUgQIDAQAB";

	/**
	 * RSA private key.
	 * You can use it to decrypt log, which was encrypted with {@link Logger#DEFAULT_PUBLIC_KEY}
	 */
	public static final String DEFAULT_PRIVATE_KEY = "MIICeQIBADANBgkqhkiG9w0BAQEFAASCAmMwggJfAgEAAoGBCizLAsaIes3Vm00rl3Pftbv+3L+cjjdcQCHq44qnpXDsbLyfhOrGbh7yVLW7LGkfTcFKv3IcM/Piow4RFUx0EsvQaPEvmMMpLgUYsLDcBhI/kHfgiaUWFsmj8Q+usKJ3W+0UcjLS88Gw6+5JKIwuM96Mt6SPvKGhExIP540S8RSBAgMBAAECgYEHjUFLmRB4nMa6UidVbEIQfyxkqK5Ie1wzmTjdku5kgxBmkASRQTLvTnarWopGJut96UVSHB5EjPGb9XfGaA0KVYhUJlN9GiN8cB4xniLt6koORihEtdZ0kAquvWWqPAFZqRsQEAqivcvcHzRFal6oxf2NPYTrZeRcftYKY3j+j0UCQQOYLnMJaqmjpsftqRXVjGOeeuEYGQhb05p7VyBKtcfFf71zPsWwWg9Z3bP0VcX03QVcxB8nEzYDUpMMJGMdUF+HAkEC1KtGx0uqCV1Y3pR9i0hoh+R1Ef3gjpEFqH1C+I1uk//TNOxb2lcAQZ/3dPuJ3JZ4UFwiZ57CKGZRaa4dQKGdtwJBAoUOGsqBIUZ9xi2OmKXI8pTIYz83XSqyHdtU6mg1IkQLFk3RtVe46oX+6wXfkxPiVL4BJi2IRBb0Le0XHPwRucUCQQCoXv878PWZ5WlvlbqxsOowoMEepAkXttREuI3l6B6IHol5I22YBlzV4pABSyxV51Qe/7kysC1Wa6eA0WaUjLhzAkEBa3mBT41uiNlLfEo39Pf6nGNifbKnzZdqs9qz58MjQ4xepKwPQ1Fkz2isEyODobGrLFka6BhNaH/xRp1N2eDPyg==";

	private static Logger instance;

	private static Logger getInstance() {
		return instance;
	}

	/**
	 * Initializes Logger with default parameters
	 * */
	public static void initialize(@NotNull Context context) throws IOException {
		initialize( new Initializer( context ) );
	}

	public static void initialize(@NotNull Initializer initializer) throws IOException {
		isDebug = initializer.isDebug;
		if ( verifyKey( initializer.rsaPublicKey ) ) {
			mRsaPublicKey = initializer.rsaPublicKey;
		}
		initialized = true;
		instance = new Logger( initializer.mContext, initializer.timerPeriod, initializer.autoFlushOnException, initializer.printErrorOnException );
	}

	/**
	 * Initializes logger. Should be called before usage
	 *
	 * @param context      application context
	 * @param rsaPublicKey RSA public key to encrypt logs. If yo do not want this, pass {@code null}.
	 *                     You can use default public ({@link Logger#DEFAULT_PUBLIC_KEY})
	 *                     and private ({@link Logger#DEFAULT_PRIVATE_KEY}) keys
	 * @param debug        If {@code true} logs also will be written in system log
	 * @throws IOException This exception will be thrown if writer couldn't create file for current session
	 */
	public static void initialize(Context context, String rsaPublicKey, boolean debug) throws IOException {
		initialize( new Initializer( context ).setRsaPublicKey( rsaPublicKey ).setDebug( debug ) );
	}

	/**
	 * Initializes logger. Should be called before usage
	 *
	 * @param context      application context
	 * @param rsaPublicKey RSA public key to encrypt logs. If yo do not want this, pass {@code null}.
	 *                     You can use default public ({@link Logger#DEFAULT_PUBLIC_KEY})
	 *                     and private ({@link Logger#DEFAULT_PRIVATE_KEY}) keys
	 * @param debug        If {@code true} logs also will be written in system log
	 * @param timerPeriod  Logger will start a timer with a period in seconds that will flush the entries in the buffer.
	 *                     Pass {@code timerPeriod} <= 0 if you don't want autoflushing.
	 *                     Default is 30.
	 * @param autoFlushOnException If true, Logger will override Thread.defaultUncaughtExceptionHandler.
	 *                             Default is true.
	 *                             <b>Note: </b> if {@code printErrorOnException} is true, this parameter automatically true.
	 * @param printErrorOnException If true, then when an exception occurs, the Logger will record it automatically with tag "Logger" and throw the exception further.
	 *                              Default is true.
	 * @throws IOException This exception will be thrown if writer couldn't create file for current session
	 */
	@Deprecated
	public static void initialize(Context context, String rsaPublicKey, boolean debug, int timerPeriod, boolean autoFlushOnException, boolean printErrorOnException) throws IOException{
		Initializer initializer = new Initializer( context );
		initializer.setDebug( debug )
				.setRsaPublicKey( rsaPublicKey )
				.setAutoFlushOnException( autoFlushOnException )
				.setPrintErrorOnException( printErrorOnException )
				.setTimerPeriod( timerPeriod );
		initialize( initializer );
	}

	private static boolean verifyKey(String stringKey) {
		if(stringKey == null)
			return false;
		X509EncodedKeySpec spec = new X509EncodedKeySpec( Base64.decode( stringKey, Base64.DEFAULT ) );
		try {
			KeyFactory.getInstance( "RSA" ).generatePublic( spec );
			return true; // if no exceptions
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private Logger(Context context, int timerPeriod, boolean autoFlushOnException, boolean printErrorOnException) throws IOException {
		mWriter = new Writer( context, mRsaPublicKey );
		if(timerPeriod > 0){
			mTimer = new Timer();
			mTimer.schedule( new TimerTask() {
				@Override
				public void run() {
					flush();
				}
			}, timerPeriod * 1000, timerPeriod * 1000 );
		}
		if(autoFlushOnException || printErrorOnException) {
			final Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
			Thread.setDefaultUncaughtExceptionHandler( (t, e)->{
				if ( printErrorOnException ) {
					e( "Logger", e.toString(), e, t );
				}
				flush();
				if ( previousHandler != null )
					previousHandler.uncaughtException( t, e );
			} );
		}
	}

	/**
	 * Function returns path for current log file
	 */
	public static String getPathForLog() {
		return getInstance().mWriter.getPath();
	}

	/**
	 * Returns lines count of information about device at the beginning of log
	 */
	public static int getAdditionalInfoLinesCount() {
		return getInstance().mWriter.getAdditionalInfoLinesCount();
	}

	/**
	 * Returns list of decrypted messages.
	 * If log is not encrypted, returns messages directly from log
	 *
	 * @param privateKey RSA private key, which log can be decrypted with
	 * @throws IOException              if there is some error during log reading
	 * @throws GeneralSecurityException if there is some error during log decryption
	 */
	public static ArrayList<String> getDecryptedLog(String privateKey) throws IOException, GeneralSecurityException {
		String path = getPathForLog();
		if ( mRsaPublicKey == null ) {
			return LogDecryptor.readFileLinesWithoutPartSeparator( path );
		}
		return new LogDecryptor( privateKey ).decryptLogFile( path );
	}

	/**
	 * Returns list of messages in log, which may be encrypted.
	 * Use {@link LogDecryptor#decryptLogFile(String)} or {@link Logger#getDecryptedLog(String)} to get decrypted log messages
	 *
	 * @throws IOException if there is some error during log reading
	 */
	public ArrayList<String> readLogFile(String pathToLog) throws IOException {
		return LogDecryptor.readFileLinesWithoutPartSeparator( pathToLog );
	}

	/**
	 * Call this function when your app is destroying.
	 * After function call, all messages in buffer will be added to writer and will be written into log
	 */
	public static void closeLogger() {
		getInstance().close();
	}

	private void close() {
		synchronized (mBuffer) {
			mWriter.addAll( mBuffer );
			mBuffer.clear();
			mWriter.close();
		}
		if(mTimer != null){
			mTimer.cancel();
			mTimer = null;
		}
		initialized = false;
	}

	@Override
	protected void finalize() throws Throwable {
		if ( initialized ) {
			close();
		}
		super.finalize();
	}

	public static void i(String tag, String message) {info( tag, message );}

	public static void info(String tag, String message) {
		if ( isDebug ) {
			Log.i( tag, message );
		}
		if ( initialized ) {
			getInstance().addRecord( "I", tag, message );
		}
	}

	public static void e(String tag, String message) {
		error( tag, message, Thread.currentThread() );
	}

	public static void e(String tag, String message, Throwable tr){
		e( tag, message, tr, Thread.currentThread() );
	}

	public static void e(String tag, String message, Throwable tr, Thread thread) {
		error( tag, message + "\n" + Log.getStackTraceString( tr ), thread );
	}

	public static void error(String tag, String message, Thread thread) {
		if ( isDebug ) {
			Log.e( tag, message );
		}
		if ( initialized ) {
			getInstance().addRecord( "E", tag, message );
		}
	}

	public static void v(String tag, String message) {verbose( tag, message );}

	public static void verbose(String tag, String message) {
		if ( isDebug ) {
			Log.v( tag, message );
		}
		if ( initialized ) {
			getInstance().addRecord( "V", tag, message );
		}
	}

	public static void d(String tag, String message) {debug( tag, message );}

	public static void debug(String tag, String message) {
		if ( isDebug ) {
			Log.d( tag, message );
		}
		if ( initialized ) {
			getInstance().addRecord( "D", tag, message );
		}
	}

	public static void w(String tag, String message) {warn( tag, message );}

	public static void warn(String tag, String message) {
		if ( isDebug ) {
			Log.w( tag, message );
		}
		if ( initialized ) {
			getInstance().addRecord( "W", tag, message );
		}
	}

	/**
	 * Flushes all messages in buffer to writer
	 */
	public static void flush() {
		getInstance().flushBuffer();
	}

	private void flushBuffer() {
		synchronized (mBuffer) {
			mWriter.addAll( mBuffer );
			mBuffer.clear();
		}
	}

	private void addRecord(String level, String tag, String message){
		addRecord( level, tag, message, Thread.currentThread() );
	}

	private void addRecord(String level, String tag, String message, Thread thread) {
		String time = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS" ).format( new Date() );
		String out = level + ":" + time + " " + Process.myUid() + " " + thread.getId() + "/" + tag + ": " + message;
		synchronized (mBuffer) {
			mBuffer.add( out );
			if ( mBuffer.size() >= BUFFER_SIZE ) {
				mWriter.addAll( mBuffer );
				mBuffer.clear();
			}
		}
	}

}
