package com.beproj.bikenav;

import java.util.List;

import android.app.IntentService;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceTransitionsIntentService extends IntentService {

	String TAG = "Geofence Debug";

	public GeofenceTransitionsIntentService() {
		super("Geofence Service");
		// Toast.makeText(this, "Service entered", Toast.LENGTH_SHORT).show();
	}

	public GeofenceTransitionsIntentService(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	protected void onHandleIntent(Intent intent) {
		GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
		if (geofencingEvent.hasError()) {
			String errorMessage = geofencingEvent.getErrorCode() + "";
			Log.e(TAG, errorMessage);
			return;
		}

		// Get the transition type.
		int geofenceTransition = geofencingEvent.getGeofenceTransition();
		List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
		Geofence thisgeo = triggeringGeofences.get(0);
		String gid = thisgeo.getRequestId();
		int gno = Integer.parseInt(gid.substring(3, gid.length()));
		Toast.makeText(this, gno+"", Toast.LENGTH_LONG).show();
		// Test that the reported transition was of interest.
		if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) 
		{
			String utterance = MapActivity.stepList.get(gno).getManeuver();
			if(utterance!="")
			{
				SpeechDirections sd = new SpeechDirections(this);
				sd.speakOut(utterance);
				Toast.makeText(this, "Geo "+gno+"", Toast.LENGTH_LONG).show();
			}
		}
		if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT)
		{
			String utterance = MapActivity.stepList.get(gno).getInstructions();
			if(utterance!="")
			{
				SpeechDirections sd = new SpeechDirections(this);
				sd.speakOut(utterance);
				Toast.makeText(this, "Geo "+gno+"", Toast.LENGTH_LONG).show();
			}
		
			
		} else {
			Log.e(TAG, "INVALID GEOFENCE TRANSITION TYPE YO!");
		}

	}
}
