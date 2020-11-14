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
			Logger.initialize( this, null, false );
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText( this, e.toString(), Toast.LENGTH_SHORT ).show();
		}
		new Thread(()->{
			for(int i = 1; i <= 10; i++){
				Logger.i( "Logger Example", "This is message number " + i + " from first thread" );
			}
		}).start();
		new Thread(()->{
			for(int i = 1; i <= 10; i++){
				Logger.i( "Logger Example", "This is message number " + i + " from second thread" );
			}
		}).start();
	}
}