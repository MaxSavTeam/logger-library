package team.maxsav.loggerexample;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

import team.maxsav.logger.Logger;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );

		try {
			Logger.initialize( this, Logger.DEFAULT_PUBLIC_KEY, false );
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText( this, e.toString(), Toast.LENGTH_SHORT ).show();
		}
		new Thread(()->{
			for(int i = 1; i <= 10; i++){
				if(i == 6){
					Logger.e( "Logger Example", "Error simulation", new Exception("Test exception") );
				}else {
					Logger.i( "Logger Example", "This is message number " + i + " from first thread" );
				}
			}
		}).start();
		new Thread(()->{
			for(int i = 1; i <= 10; i++){
				if(i == 5){
					Logger.i( "Logger Example", "Very long message jbdfusgvoszvgidzblvkhjbzkdhvbkhjzdbzhbdkhbvluihoeirughUIEHgoizufhrfdbvfdbv zkjhfbvkzjhbd khjbzkdhjb khjbgkurbfgogefoGEFHgoudvglhBSLDVHJbhjvboargviuhrpiuvbhpizvfuhdrpziufhhdlivbzvzhjfdbkljhzborefgbdvlzhifdgvjhzfd" );
				}else {
					Logger.i( "Logger Example", "This is message number " + i + " from second thread" );
				}
			}
		}).start();

		findViewById( R.id.btnViewLog ).setOnClickListener( view->{
			try {
				ArrayList<String> messages = Logger.getDecryptedLog( Logger.DEFAULT_PRIVATE_KEY );
				String message = "";
				for(String s : messages){
					message = String.format( "%s%s\n", message, s );
				}

				AlertDialog.Builder builder = new AlertDialog.Builder( this );
				builder.setMessage( message )
						.setPositiveButton( "OK", (dialog, i)->dialog.cancel() )
						.setCancelable( false );
				builder.show();
			} catch (IOException | GeneralSecurityException e) {
				Toast.makeText( this, e.toString(), Toast.LENGTH_LONG ).show();
				e.printStackTrace();
			}
		} );
	}
}