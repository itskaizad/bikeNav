package com.beproj.bikenav;

import com.google.android.gms.maps.model.LatLng;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.Toast;

public class GPSTracker extends Service implements LocationListener{

	private final Context context;
	
	boolean isGPSEnabled = false;
	boolean isNetworkEnabled = false;
	boolean canGetLocation = false;
	
	Location location;
	
	double latitude;
	double longitude;
	
	private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0;
	private static final long MIN_TIME_BW_UPDATES = 1000;
	
	protected LocationManager locationManager;
	
	public GPSTracker(Context context) {
		this.context = context;
		getLocation();
	}
	
	public Location getLocation() {
		try {
			locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
			
			isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
			
			isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			
			if(!isGPSEnabled && !isNetworkEnabled) {
				showSettingsAlert();
			} else {
				this.canGetLocation = true;
				
				if (isNetworkEnabled) {
					
					locationManager.requestLocationUpdates(
							LocationManager.NETWORK_PROVIDER,
							MIN_TIME_BW_UPDATES,
							MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

					if (locationManager != null) {
						location = locationManager
								.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

						if (location != null) {
							
							latitude = location.getLatitude();
							longitude = location.getLongitude();
						}
					}

				}
				
				if(isGPSEnabled) {
					if(location == null) {
						locationManager.requestLocationUpdates(
								LocationManager.GPS_PROVIDER,
								MIN_TIME_BW_UPDATES,
								MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
						
						if(locationManager != null) {
							location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
							
							if(location != null) {
								latitude = location.getLatitude();
								longitude = location.getLongitude();
							}
						}
					}
				}
				
				
 			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return location;
	}
	
	
	public void stopUsingGPS() {
		if(locationManager != null) {
			locationManager.removeUpdates(GPSTracker.this);
		}
	}
	
	public double getLatitude() {
		if(location != null) {
			latitude = location.getLatitude();
		}
		return latitude;
	}
	
	public double getLongitude() {
		if(location != null) {
			longitude = location.getLongitude();
		}
		
		return longitude;
	}
	
	public boolean canGetLocation() {
		return this.canGetLocation;
	}
	
	public void showSettingsAlert() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
		
		alertDialog.setTitle("GPS is disabled!");
		
		alertDialog.setMessage("This app requires you to switch on GPS.\n\nDo that now?");
		
		alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				context.startActivity(intent);
			}
		});
		
		alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		
		alertDialog.show();
	}
	
	@Override
	public void onLocationChanged(Location loc) {
		// TODO Auto-generated method stub
		location = loc;
		Toast.makeText(context, "New " + loc.getProvider() + " location in tow", Toast.LENGTH_LONG).show();
		LatLng send = new LatLng(loc.getLatitude(), loc.getLongitude());
		//SpeechDirections sd = new SpeechDirections(context);
		//sd.speakOut("Yay!");
		if(MapActivity.currentMarker!= null)
		{
			MapActivity.updateMarker(send);
		}
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		Toast.makeText(context, arg0+" : Provider disabled.", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		Toast.makeText(context, arg0+" : Provider enabled.", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
