package com.beproj.bikemusic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beproj.bikemusic.R;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class AlbumList extends Activity {
	public static final String ALBUM_NAME = "ALBUM_NAME";

	private static final String TAG = "AlbumList";
	private List<Map<String,String>> albums;
	private BaseAdapter listAdapter;

	private String currentTheme;
	private String currentSize;

	private BroadcastReceiver exitReceiver;
	
	private void populateAlbums(String artistName, String artistPath){
		albums = new ArrayList<Map<String,String>>();
		
		File artist = new File(artistPath);
		Log.d(TAG, "storage directory = " + artist);
		if(!artist.isDirectory() || (artist.listFiles() == null)){
			Log.e(TAG, "Invalid artist directory provided: " +  artistPath);
			Toast.makeText(getApplicationContext(), "The selected directory is empty", Toast.LENGTH_SHORT).show();
			return;
		}
		
		List<File> albumFiles = new ArrayList<File>();
		for(File albumFile : artist.listFiles()){
			if(Utils.isValidAlbumDirectory(albumFile)){
				albumFiles.add(albumFile);
			} else {
				Log.v(TAG, "Found invalid album " + albumFile);
			}
		}
		
		Collections.sort(albumFiles, new Comparator<File>(){

			@Override
			public int compare(File lhs, File rhs) {
				return lhs.getName().compareTo(rhs.getName());
			}
			
		});
		
		albumFiles.add(0,new File("All"));
		
		for(File albumFile : albumFiles){
			String album = albumFile.getName();
			Log.v(TAG, "Adding album " + album);
			Map<String,String> map = new HashMap<String, String>();
			map.put("album", album);			
			albums.add(map);
		}
		
		// If albums size is 1, then there were no directories in this folder.
		// skip straight to listing songs.
		if(albums.size() == 1){
       	 Intent intent = new Intent(AlbumList.this, SongList.class);
		 intent.putExtra(ALBUM_NAME, "All");
       	 intent.putExtra(ArtistList.ARTIST_NAME, artist.getName());
       	 intent.putExtra(ArtistList.ARTIST_ABS_PATH_NAME, artistPath);
       	 startActivity(intent);
       	 // In this case we don't want to add the AlbumList to the back stack
       	 // so call 'finish' immediately.
       	 finish();
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	    Intent intent = getIntent();
	    final String artist = intent.getStringExtra(ArtistList.ARTIST_NAME);
		
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle(artist);
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = sharedPref.getString("pref_theme", getString(R.string.light));
        String size = sharedPref.getString("pref_text_size", getString(R.string.medium));
        Log.i(TAG, "got configured theme " + theme);
        Log.i(TAG, "got configured size " + size);
        currentTheme = theme;
        currentSize = size;
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
		setContentView(R.layout.activity_album_list);
		
		 // Get the message from the intent
	    Log.i(TAG, "Getting albums for " + artist);
	    
	    final String artistPath = intent.getStringExtra(ArtistList.ARTIST_ABS_PATH_NAME);
	    populateAlbums(artist, artistPath);
        
        listAdapter = new SimpleAdapter(this, albums, R.layout.pgmp_list_item, new String[] {"album"}, new int[] {R.id.PGMPListItemText});
	    ListView lv = (ListView) findViewById(R.id.albumListView);
        lv.setAdapter(listAdapter);
        
        // React to user clicks on item
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

             public void onItemClick(AdapterView<?> parentAdapter, View view, int position,
                                     long id) {
            	 TextView clickedView = (TextView) view.findViewById(R.id.PGMPListItemText);;
            	 Intent intent = new Intent(AlbumList.this, SongList.class);
            	 intent.putExtra(ALBUM_NAME, clickedView.getText());
            	 intent.putExtra(ArtistList.ARTIST_NAME, artist);
            	 intent.putExtra(ArtistList.ARTIST_ABS_PATH_NAME, artistPath);
            	 startActivity(intent);
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
    	unregisterReceiver(exitReceiver);
    	super.onDestroy();
	}
	
    @Override
	protected void onResume() {
		super.onResume();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = sharedPref.getString("pref_theme", getString(R.string.light));
        String size = sharedPref.getString("pref_text_size", getString(R.string.medium));
        Log.i(TAG, "got configured theme " + theme);
        Log.i(TAG, "Got configured size " + size);
        if(currentTheme == null){
        	currentTheme = theme;
        } 
        
        if(currentSize == null){
        	currentSize = size;
        }
        if(!currentTheme.equals(theme) || !currentSize.equals(size)){
        	finish();
        	startActivity(getIntent());
        }
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.album_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
        	Intent intent = new Intent(AlbumList.this, SettingsActivity.class);
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

}
