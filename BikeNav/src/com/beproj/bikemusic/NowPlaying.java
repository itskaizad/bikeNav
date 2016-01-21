package com.beproj.bikemusic;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.beproj.bikemusic.R;
import com.beproj.bikemusic.MusicPlaybackService.PlaybackState;

public class NowPlaying extends Activity {

	private static final String TAG = "Now Playing";
	static final String KICKOFF_SONG = "KICKOFF_SONG";

	// State information
	private String desiredArtistName;
	private String desiredArtistAbsPath;
	private String desiredAlbumName;
	private String[] desiredSongAbsFileNames;
	private int desiredAbsSongFileNamesPosition;
	private boolean startPlayingRequired = true;
	private boolean userDraggingProgress = false;

	// Messaging and service stuff
	boolean mIsBound;
	private Messenger mService;
	final Messenger mMessenger = new Messenger(new IncomingHandler(this));
	private ServiceConnection mConnection = new NowPlayingServiceConnection(this);
	private String currentTheme;
	private String currentSize;
	private boolean currentFullScreen;
	private int desiredSongProgress;
	private BroadcastReceiver exitReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent originIntent = getIntent();
		if(originIntent.getBooleanExtra("From_Notification", false)){

			String artistName = originIntent.getStringExtra(ArtistList.ARTIST_NAME);
			String artistAbsPath = originIntent.getStringExtra(ArtistList.ARTIST_ABS_PATH_NAME);
			if(artistName != null && artistAbsPath != null){
				Log.i(TAG, "Now Playing was launched from a notification, setting up its back stack");
				// Reference: https://developer.android.com/reference/android/app/TaskStackBuilder.html
				TaskStackBuilder tsb = TaskStackBuilder.create(this);
				Intent intent = new Intent(this, ArtistList.class);
				tsb.addNextIntent(intent);

				intent = new Intent(this, AlbumList.class);
				intent.putExtra(ArtistList.ARTIST_NAME, artistName);
				intent.putExtra(ArtistList.ARTIST_ABS_PATH_NAME, artistAbsPath);
				tsb.addNextIntent(intent);

				String albumName =  originIntent.getStringExtra(AlbumList.ALBUM_NAME);
				if(albumName != null){
					intent = new Intent(this, SongList.class);
					intent.putExtra(AlbumList.ALBUM_NAME, albumName);
					intent.putExtra(ArtistList.ARTIST_NAME, artistName);
					intent.putExtra(ArtistList.ARTIST_ABS_PATH_NAME, artistAbsPath);
					tsb.addNextIntent(intent);
				}
				intent = new Intent(this, NowPlaying.class);
				tsb.addNextIntent(intent);
				tsb.startActivities();
			}

		}

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String theme = sharedPref.getString("pref_theme", getString(R.string.light));
		String size = sharedPref.getString("pref_text_size", getString(R.string.medium));
		Log.i(TAG, "got configured theme " + theme);
		Log.i(TAG, "got configured size " + size);
		
        // These settings were fixed in english for a while, so check for old style settings as well as language specific ones.
        if(theme.equalsIgnoreCase(getString(R.string.dark)) || theme.equalsIgnoreCase("dark")){
        	Log.i(TAG, "setting theme to " + theme);
        	if(size.equalsIgnoreCase(getString(R.string.small)) || size.equalsIgnoreCase("small")){
        		setTheme(R.style.PGMPDarkSmall);
        	} else if (size.equalsIgnoreCase(getString(R.string.medium)) || size.equalsIgnoreCase("medium")){
        		setTheme(R.style.PGMPDarkMedium);
        	} else {
        		setTheme(R.style.PGMPDarkLarge);
        	}
        } else if (theme.equalsIgnoreCase(getString(R.string.light)) || theme.equalsIgnoreCase("light")){
        	Log.i(TAG, "setting theme to " + theme);
        	if(size.equalsIgnoreCase(getString(R.string.small)) || size.equalsIgnoreCase("small")){
        		setTheme(R.style.PGMPLightSmall);
        	} else if (size.equalsIgnoreCase(getString(R.string.medium)) || size.equalsIgnoreCase("medium")){
        		setTheme(R.style.PGMPLightMedium);
        	} else {
        		setTheme(R.style.PGMPLightLarge);
        	}
        }

		boolean fullScreen = sharedPref.getBoolean("pref_full_screen_now_playing", false);
		currentFullScreen = fullScreen;
		if(fullScreen){
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		setContentView(R.layout.activity_now_playing);

		if(savedInstanceState == null){
			doBindService(true);
			startPlayingRequired = true;
		} else {
			doBindService(false);
			startPlayingRequired = false;
		}

		// Get the message from the intent
		Intent intent = getIntent();
		if(intent.getBooleanExtra(KICKOFF_SONG, false)){
			desiredArtistName = intent.getStringExtra(ArtistList.ARTIST_NAME);
			desiredAlbumName = intent.getStringExtra(AlbumList.ALBUM_NAME);
			desiredArtistAbsPath = intent.getStringExtra(ArtistList.ARTIST_ABS_PATH_NAME);
			desiredSongAbsFileNames = intent.getStringArrayExtra(SongList.SONG_ABS_FILE_NAME_LIST);
			desiredAbsSongFileNamesPosition = intent.getIntExtra(SongList.SONG_ABS_FILE_NAME_LIST_POSITION, 0);
			desiredSongProgress = intent.getIntExtra(MusicPlaybackService.TRACK_POSITION, 0);

			Log.d(TAG, "Got song names " + desiredSongAbsFileNames + " position "
					+ desiredAbsSongFileNamesPosition);

			TextView et = (TextView) findViewById(R.id.artistName);
			et.setText(desiredArtistName);

			et = (TextView) findViewById(R.id.albumName);
			et.setText(desiredAlbumName);
		}

		// The song name field will be set when we get our first update update from the service.

		final ImageButton pause = (ImageButton) findViewById(R.id.playPause);
		pause.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				playPause();
			}

		});

		ImageButton previous = (ImageButton) findViewById(R.id.previous);
		previous.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				previous();
			}

		});
		
		previous.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				jumpBack();
				return true;
			}
		});

		ImageButton next = (ImageButton) findViewById(R.id.next);
		next.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				next();
			}
		});

		final ImageButton shuffle = (ImageButton) findViewById(R.id.shuffle);
		shuffle.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				toggleShuffle();
			}
		});
		
		final ImageButton jumpback = (ImageButton) findViewById(R.id.jumpback);
		jumpback.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				jumpBack();
			}
		});

		SeekBar seekBar = (SeekBar)findViewById(R.id.songProgressBar);
		seekBar.setEnabled(true);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

			private int requestedProgress;

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				if(fromUser){
					Log.v(TAG, "drag location updated..." + progress);
					this.requestedProgress = progress;
					updateSongProgressLabel(progress);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				NowPlaying.this.userDraggingProgress = true;

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				Message msg = Message.obtain(null, MusicPlaybackService.MSG_SEEK_TO);
				msg.getData().putInt(MusicPlaybackService.TRACK_POSITION, requestedProgress);
				try {
					Log.i(TAG, "Sending a request to seek!");
					mService.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				NowPlaying.this.userDraggingProgress = false;
			}

		});
		
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.smithdtyler.ACTION_EXIT");
        exitReceiver = new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				Log.i(TAG, "Received exit request, shutting down...");
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
		super.onDestroy();
		unregisterReceiver(exitReceiver);
		unbindService(mConnection);
	}

	private void updateSongProgressLabel(int progress){
		TextView progressLabel = (TextView)findViewById(R.id.songProgressLabel);
		int minutes = progress / (1000 * 60);
		int seconds = (progress % (1000 * 60)) / 1000;
		String time = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
		progressLabel.setText(time);
	}

	// Playback control methods
	private void playPause(){
		Log.d(TAG, "Play/Pause clicked...");
		Message msg = Message.obtain(null, MusicPlaybackService.MSG_PLAYPAUSE);
		try {
			Log.i(TAG, "Sending a request to start playing!");
			mService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void next(){
		Log.d(TAG, "next...");
		Message msg = Message.obtain(null, MusicPlaybackService.MSG_NEXT);
		try {
			Log.i(TAG, "SEnding a request to go to next!");
			mService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void previous(){
		Log.d(TAG, "Previous clicked...");
		Message msg = Message.obtain(null, MusicPlaybackService.MSG_PREVIOUS);
		try {
			Log.i(TAG, "Sending a request to go to previous!");
			mService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void jumpBack(){
		Log.d(TAG, "JumpBack clicked...");
		Message msg = Message.obtain(null, MusicPlaybackService.MSG_JUMPBACK);
		try {
			Log.i(TAG, "Sending a request to jump back!");
			mService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void toggleShuffle(){
		Log.d(TAG, "Shuffle clicked...");
		Message msg = Message.obtain(null, MusicPlaybackService.MSG_TOGGLE_SHUFFLE);
		try {
			Log.i(TAG, "Sending a request to toggle shuffle!");
			mService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	// Service connection management
	private class NowPlayingServiceConnection implements ServiceConnection {

		private NowPlaying _nowPlaying;

		public NowPlayingServiceConnection(NowPlaying nowPlaying) {
			this._nowPlaying = nowPlaying;
		}

		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);

			// Register with the service
			try {
				Message msg = Message.obtain(null,
						MusicPlaybackService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
				// In this case the service has crashed before we could even do
				// anything with it
			}

			if(this._nowPlaying.startPlayingRequired){
				if(desiredSongAbsFileNames != null){
					// set the playlist
					Message msg = Message.obtain(null, MusicPlaybackService.MSG_SET_PLAYLIST);
					msg.getData().putStringArray(SongList.SONG_ABS_FILE_NAME_LIST, desiredSongAbsFileNames);
					msg.getData().putInt(SongList.SONG_ABS_FILE_NAME_LIST_POSITION, desiredAbsSongFileNamesPosition);
					msg.getData().putString(ArtistList.ARTIST_NAME, desiredArtistName);
					msg.getData().putString(ArtistList.ARTIST_ABS_PATH_NAME, desiredArtistAbsPath);
					msg.getData().putString(AlbumList.ALBUM_NAME, desiredAlbumName);
					msg.getData().putInt(MusicPlaybackService.TRACK_POSITION, desiredSongProgress);
					try {
						Log.i(TAG, "Sending a playlist!");
						mService.send(msg);
					} catch (RemoteException e) {
						e.printStackTrace();
					}

					// start playing!
					msg = Message.obtain(null, MusicPlaybackService.MSG_PLAYPAUSE);
					try {
						Log.i(TAG, "Sending a play command!");
						mService.send(msg);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}

		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected - process crashed.
			mService = null; // TODO need to do some null checks
		}
	};

	private static class IncomingHandler extends Handler {

		private NowPlaying _activity;

		private IncomingHandler(NowPlaying nowPlaying){
			_activity = nowPlaying;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MusicPlaybackService.MSG_SERVICE_STATUS:
				String currentSongName = msg.getData().getString(MusicPlaybackService.PRETTY_SONG_NAME);
				TextView tv = (TextView) _activity.findViewById(R.id.songName);
				if(!tv.getText().equals(currentSongName)){
					tv.setText(currentSongName);
				}

				String currentAlbumName = msg.getData().getString(MusicPlaybackService.PRETTY_ALBUM_NAME);
				tv = (TextView) _activity.findViewById(R.id.albumName);
				if(!tv.getText().equals(currentAlbumName)){
					tv.setText(currentAlbumName);
				}

				String currentArtistName = msg.getData().getString(MusicPlaybackService.PRETTY_ARTIST_NAME);
				tv = (TextView) _activity.findViewById(R.id.artistName);
				if(!tv.getText().equals(currentArtistName)){
					tv.setText(currentArtistName);
				}

				boolean isShuffling = msg.getData().getBoolean(MusicPlaybackService.IS_SHUFFLING);
				ImageButton shuffle = (ImageButton)_activity.findViewById(R.id.shuffle);
				if(shuffle.isSelected() != isShuffling){
					shuffle.setSelected(isShuffling);
				}

				PlaybackState state = PlaybackState.values()[msg.getData().getInt(MusicPlaybackService.PLAYBACK_STATE, 0)];
				ImageButton playPause = (ImageButton)_activity.findViewById(R.id.playPause);
				if(playPause.getContentDescription().equals(_activity.getResources().getString(R.string.play))){
					if(state == PlaybackState.PLAYING){
						playPause.setImageDrawable(_activity.getResources().getDrawable(R.drawable.ic_action_pause));
						playPause.setContentDescription(_activity.getResources().getString(R.string.pause));
					}
				} else {
					if(state == PlaybackState.PAUSED){
						playPause.setImageDrawable(_activity.getResources().getDrawable(R.drawable.ic_action_play));
						playPause.setContentDescription(_activity.getResources().getString(R.string.play));
					}
				}
				int duration = msg.getData().getInt(MusicPlaybackService.TRACK_DURATION, -1);
				int position = msg.getData().getInt(MusicPlaybackService.TRACK_POSITION, -1);
				if(duration > 0){
					if(!_activity.userDraggingProgress){
						SeekBar seekBar = (SeekBar)_activity.findViewById(R.id.songProgressBar);
						seekBar.setMax(duration);
						seekBar.setProgress(position);
						_activity.updateSongProgressLabel(position);
					}
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	// Service Management Methods
	@SuppressLint("InlinedApi") 
	void doBindService(boolean startService) {
		Log.i(TAG, "Binding to the service!");
		bindService(new Intent(this, MusicPlaybackService.class), mConnection,
				Context.BIND_IMPORTANT | Context.BIND_AUTO_CREATE);
		mIsBound = true;
		// Need to start the service so it won't be stopped when this activity is destroyed.
		// https://developer.android.com/guide/components/bound-services.html
		if(startService){
			startService(new Intent(this, MusicPlaybackService.class));
		}
	}

	void doUnbindService() {
		Log.i(TAG, "Unbinding the service!");
		if (mIsBound) {
			// If we have received the service, and hence registered with it,
			// then now is the time to unregister.
			if (mService != null) {
				try {
					Message msg = Message.obtain(null,
							MusicPlaybackService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service has
					// crashed.
				}
			}

			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.now_playing, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent intent = new Intent(NowPlaying.this, SettingsActivity.class);
			startActivity(intent);
			return true;
		}
		if (id == R.id.action_exit) {
			Intent broadcastIntent = new Intent();
			broadcastIntent.setAction("com.smithdtyler.ACTION_EXIT");
			sendBroadcast(broadcastIntent);
			Intent startMain = new Intent(Intent.ACTION_MAIN);
		    startMain.addCategory(Intent.CATEGORY_HOME);
		    startMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		    startActivity(startMain);
			finish();
            return true;
        }
        if(id == android.R.id.home){
        	onBackPressed();
        	return true;
        }
		return super.onOptionsItemSelected(item);
	}

	private void setAccentColors(int color){
		View shuffleBG = findViewById(R.id.shufflebackground);
		View buttonBG = findViewById(R.id.playpausepreviousbackground);
		View spacer1 = findViewById(R.id.spacer1);
		View spacer2 = findViewById(R.id.spacer2);
		if(shuffleBG != null && buttonBG != null && spacer1 != null && spacer2 != null){
			shuffleBG.setBackgroundColor(color);
			buttonBG.setBackgroundColor(color);
			spacer1.setBackgroundColor(color);
			spacer2.setBackgroundColor(color);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

		String accentColor = sharedPref.getString("accent_color", getResources().getStringArray(R.array.accentcoloroptions)[0]);
		String customAccentColor = sharedPref.getString("custom_accent_color", "");
		String colorOptions[] = getResources().getStringArray(R.array.accentcoloroptions);
		// TODO I'm sure there's a better way to do this, I'm just not sure what it is...
		for(int i = 0;i<colorOptions.length;i++){
			if(accentColor.equals(colorOptions[i])){
				switch(i){
				case 0: 
					setAccentColors(getResources().getColor(R.color.agategrey));
					break;
				case 1: 
					setAccentColors(getResources().getColor(R.color.blackgreen));
					break;
				case 2: 
					setAccentColors(getResources().getColor(R.color.brilliantblue));
					break;
				case 3: 
					setAccentColors(getResources().getColor(R.color.brilliantorange));
					break;
				case 4: 
					setAccentColors(getResources().getColor(R.color.emeraldgreen));
					break;
				case 5: 
					setAccentColors(getResources().getColor(R.color.ivory));
					break;
				case 6: 
					setAccentColors(getResources().getColor(R.color.skyblue));
					break;
				case 7: 
					setAccentColors(getResources().getColor(R.color.trafficgreen));
					break;
				case 8: 
					setAccentColors(getResources().getColor(R.color.waterblue));
					break;
				case 9: 
					try{
						Log.i(TAG, "custom color: " + customAccentColor);
						if(!customAccentColor.startsWith("#")){
							customAccentColor = "#" + customAccentColor;
						}
						if(customAccentColor.toLowerCase(Locale.getDefault()).startsWith("0x")){
							customAccentColor = customAccentColor.substring(2);
						}
						int custom = Color.parseColor(customAccentColor.trim());
						setAccentColors(custom);
					} catch (Exception e){
						Log.w(TAG, "Unable to parse custom color", e);
					}
					break;
				}
			}
		}
		
		String theme = sharedPref.getString("pref_theme", getString(R.string.light));
		String size = sharedPref.getString("pref_text_size", getString(R.string.medium));
		boolean fullScreen = sharedPref.getBoolean("pref_full_screen_now_playing", false);
		Log.i(TAG, "got configured theme " + theme);
		Log.i(TAG, "Got configured size " + size);
		if(currentTheme == null){
			currentTheme = theme;
		} 

		if(currentSize == null){
			currentSize = size;
		}
		if(!currentTheme.equals(theme) || !currentSize.equals(size) || currentFullScreen != fullScreen){
			finish();
			startActivity(getIntent());
		}
	}


}
