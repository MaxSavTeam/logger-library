package team.maxsav.loggerexample;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

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
		long millis = System.currentTimeMillis();
		for(int i = 0; i <= 10; i++){
			Logger.i( "TEST", "Message " + i );
		}
		Toast.makeText( this, "" + (System.currentTimeMillis() - millis), Toast.LENGTH_SHORT ).show();
	}
}