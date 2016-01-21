package com.beproj.bikenav;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.beproj.bikemusic.MusicPlaybackService;

public class MainActivity extends Activity implements OnClickListener {
	
	LinearLayout music, nav;
	TextView welcome;
	private int REQUEST_ENABLE_BT;
	private BroadcastReceiver exitReceiver;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		//ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        //toneG.startTone(ToneGenerator.TONE_SUP_INTERCEPT, 1000);

		music = (LinearLayout)findViewById(R.id.getMusic);
		music.setOnClickListener(this);
		nav = (LinearLayout)findViewById(R.id.getNav);
		nav.setOnClickListener(this);
		
		IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);//"android.intent.action.MEDIA_BUTTON"
	       MediaButtonIntentReceiver r = new MediaButtonIntentReceiver();
	       filter.setPriority(1000000);
	    ((AudioManager)getSystemService(AUDIO_SERVICE)).registerMediaButtonEventReceiver(new ComponentName(
	            this,
	            MediaButtonIntentReceiver.class));
		
		Typeface tf = Typeface.createFromAsset(getAssets(), "ClementeExtraLight.ttf");
		welcome = (TextView)findViewById(R.id.Welcome);
		welcome.setTypeface(tf);
		
		TelephonyManager TelephonyMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		TelephonyMgr.listen(new CallStateListener(this), PhoneStateListener.LISTEN_CALL_STATE);
		
		//INITIALIZE BLUETOOTH CALLS
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
		    // Device does not support Bluetooth
		}
		
		if (!mBluetoothAdapter.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		
		IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.smithdtyler.ACTION_EXIT");
        exitReceiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				Log.i("Nav Main", "Received exit request, shutting down...");
				Intent msgIntent = new Intent(getBaseContext(), MusicPlaybackService.class);
				msgIntent.putExtra("Message", MusicPlaybackService.MSG_STOP_SERVICE);
				startService(msgIntent);
				finish();
			}
        	
        };
        registerReceiver(exitReceiver, intentFilter);

		
	}
	
	@Override
	protected void onDestroy() {
    	unregisterReceiver(exitReceiver);
    	super.onDestroy();
	}
	
	// If the back key is pressed, ask if they really want to quit
    // if they do, pass the key press along. If they don't,
    // eat it.
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	Log.i("Main", " handling on key down");
        switch(keyCode)
        {
        case KeyEvent.KEYCODE_BACK:
            AlertDialog.Builder ab = new AlertDialog.Builder(MainActivity.this);
            ab.setMessage("Are you sure?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					Log.d("Main quit", "User actually wants to quit");
					// Kill the service
					Intent msgIntent = new Intent(getBaseContext(), MusicPlaybackService.class);
					msgIntent.putExtra("Message", MusicPlaybackService.MSG_STOP_SERVICE);
					startService(msgIntent);
					finish();
				}
            	
            })
            .setNegativeButton("No", new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					Log.d("Main quit", "User doesn't actually want to quit");
				}
            	
            }).show();
            return true;// Consume the event so "back" isn't actually fired.
        }

        return super.onKeyDown(keyCode, event);
    }


	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(v==music)
		{
			Intent in=new Intent(this, com.beproj.bikemusic.ArtistList.class);
			startActivity(in);
		}
			//Toast.makeText(this, "Oops! We're still working on this.", Toast.LENGTH_LONG).show();
		if(v==nav)
		{
			Intent in=new Intent(this, MapActivity.class);
			startActivity(in);
		}
		
	}

}
