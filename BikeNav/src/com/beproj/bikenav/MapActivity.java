package com.beproj.bikenav;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapActivity extends FragmentActivity implements
		SensorEventListener, GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {
	static GoogleMap map;
	Location myLocation;
	GpsStatus gpsStatus;
	AutoCompleteTextView placeIn;
	Geocoder gc;
	ArrayList<Geofence> geofenceList = new ArrayList<Geofence>();
	List<Address> list;
	ArrayList<String> stringList = new ArrayList<String>();
	LocationManager locationManager;
	GPSTracker myGps;
	static Marker currentMarker;
	Marker destMarker;
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private Sensor mMagnetic;
	float[] mGravity;
	float[] mGeomagnetic;
	TextView time, dist, summary;
	static ArrayList<RouteStep> stepList;
	BikeRouteAdapter stepListAdapter;
	ListView itenaryListView;
	LinearLayout sumbar;
	ImageView locB,menuB;
	GoogleApiClient mGoogleApiClient;
	PendingIntent mGeofencePendingIntent;
	private boolean visible = false;
	SpeechDirections sd;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mapview);

		time = (TextView) findViewById(R.id.navTime);
		dist = (TextView) findViewById(R.id.navDist);
		summary = (TextView) findViewById(R.id.navSummary);
		Typeface tf = Typeface.createFromAsset(getAssets(), "ClementeExtraLight.ttf");
		time.setTypeface(tf);
		dist.setTypeface(tf);
		tf = Typeface.createFromAsset(getAssets(), "RaspoutineMedium.otf");
		summary.setTypeface(tf);
		
		sd = new SpeechDirections(this);
		//sd.speakOut("Hello my name is Obama.");
		
		locB = (ImageView)findViewById(R.id.locButton);
		menuB = (ImageView)findViewById(R.id.menuButton);

		itenaryListView = (ListView) findViewById(R.id.listLayout);
		sumbar = (LinearLayout) findViewById(R.id.summaryBar);

		//mapCheck();
		
		
		myGps = new GPSTracker(this);
		gc = new Geocoder(this, Locale.getDefault());

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mSensorManager.registerListener(this, mAccelerometer,
				SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, mMagnetic,
				SensorManager.SENSOR_DELAY_GAME);

		placeIn = (AutoCompleteTextView) findViewById(R.id.placeInput);
		
		Intent caller = getIntent();
		try{
			String location = caller.getStringExtra("place_mic");
			placeIn.setText(location);
			hideAndGo(placeIn);
		}
		catch(Exception e)
		{
			
		}

		

		placeIn.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub

				SuggestionsAsync task = new SuggestionsAsync();
				task.execute(placeIn.getText() + "");
				try {
					list = task.get();

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (list != null) {
					Toast.makeText(getApplicationContext(), "working", 500)
							.show();
					stringList.clear();
					for (int i = 0; i < list.size(); i++) {
						String data = "";
						for (int j = 0; j < list.get(i)
								.getMaxAddressLineIndex(); j++)
							data += list.get(i).getAddressLine(j) + " ";
						stringList.add(i, data);
					}
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(
							MapActivity.this,
							android.R.layout.simple_dropdown_item_1line,
							stringList);
					placeIn.setAdapter(adapter);
				} else {
					Toast.makeText(getApplicationContext(),
							"Suggestions need network!", Toast.LENGTH_SHORT)
							.show();
				}

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
			}
		});

		// Get a handle to the Map Fragment
		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
				.getMap();

		myLocation = myGps.getLocation();
		if (myLocation != null) {
			CameraPosition cp = new CameraPosition.Builder()
					.tilt(60)
					.target(new LatLng(myLocation.getLatitude(), myLocation
							.getLongitude())).zoom(18).build();
			map.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
			MarkerOptions mOps = new MarkerOptions();
			mOps.position(new LatLng(myLocation.getLatitude(), myLocation
					.getLongitude()));
			mOps.flat(true);
			mOps.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker));
			mOps.anchor((float) 0.5, (float) 0.5);
			currentMarker = map.addMarker(mOps);
		}

		buildGoogleApiClient();

	}

	protected synchronized void buildGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API)
				.build();
	}

	public void locButton(View v) {
		myLocation = myGps.getLocation();
		CameraPosition cp = new CameraPosition.Builder()
				.tilt(60)
				.target(new LatLng(myLocation.getLatitude(), myLocation
						.getLongitude())).zoom(18).build();
		map.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
		updateMarker(new LatLng(myLocation.getLatitude(), myLocation
						.getLongitude()));
	}
	
	public void menuButton(View v)
	{
		if(visible)
		{
			visible = false;
			sumbar.setVisibility(View.GONE);
			itenaryListView.setVisibility(View.GONE);
		}
		else
		{
			visible  = true;
			sumbar.setVisibility(View.VISIBLE);
			itenaryListView.setVisibility(View.VISIBLE);
			itenaryListView.setAlpha((float) 0.8);
		}
	}

	public void doMap(View v) {
		map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
	}

	public void doSat(View v) {
		map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
	}

	public void doHyb(View v) {
		map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
	}

	public void hideAndGo(View v) {
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
		try {
			list = gc.getFromLocationName(placeIn.getText() + "", 1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (list != null) {
			map.clear();
			map.setTrafficEnabled(true);
			myLocation = myGps.getLocation();

			LatLng fromPos = new LatLng(myLocation.getLatitude(),
					myLocation.getLongitude());
			LatLng toPos = new LatLng(list.get(0).getLatitude(), list.get(0)
					.getLongitude());

			MarkerOptions mOps = new MarkerOptions();
			mOps.position(fromPos);
			mOps.flat(true);
			mOps.icon(BitmapDescriptorFactory.fromResource(R.drawable.nav2));
			mOps.anchor((float) 0.5, (float) 0.5);
			currentMarker = map.addMarker(mOps);
			destMarker = map.addMarker(new MarkerOptions().position(toPos));

			GMapV2Direction md = new GMapV2Direction();
			Document doc = md.getDocument(fromPos, toPos,
					GMapV2Direction.MODE_DRIVING, this);
			ArrayList<LatLng> directionPoint = md.getDirection(doc);
			PolylineOptions rectLine = new PolylineOptions().width(12).color(
					Color.RED);

			stepList = md.getItenary(doc);
			stepListAdapter = new BikeRouteAdapter(stepList, this);
			itenaryListView.setAdapter(stepListAdapter);
			time.setText(md.getDurationText(doc) + "");
			dist.setText(" - " + md.getDistanceText(doc) + " - ");
			summary.setText("via " + md.getSummary(doc));

			for (int i = 0; i < directionPoint.size(); i++) {
				rectLine.add(directionPoint.get(i));
			}
			map.addPolyline(rectLine);

			addGeos(stepList);
			
			new java.util.Timer().schedule(
	                new java.util.TimerTask() {
	                    @Override
	                    public void run() {
	                        // your code here
	                        callApiInit();
	                    }
	                },
	                2000);
			
			String utterance = MapActivity.stepList.get(0).getInstructions();
			if(utterance!="")
			{
				sd.speakOut(utterance);
			}
			
			
		} else
			Toast.makeText(this, "Need network for directions!",
					Toast.LENGTH_LONG).show();

	}
	
	public void callApiInit()
	{
		if (!mGoogleApiClient.isConnected()) {
            //Toast.makeText(this, "API Client not connected yet!", Toast.LENGTH_SHORT).show();
            Log.d("API Client", "Client not connected bruh!");
			return;
        }

		try{
			LocationServices.GeofencingApi.addGeofences( mGoogleApiClient,
					getGeofencingRequest(), getGeofencePendingIntent()
					).setResultCallback(this);
			Log.d("API Client", "Client CONNECTED bruh!");
		}catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            Log.i("API Exception", securityException+"");
        }
	}

	public static void updateMarker(LatLng updated) {
		currentMarker.setPosition(updated);
		CameraPosition cp = new CameraPosition.Builder().tilt(60)
				.target(updated).zoom(18).build();
		map.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
	}

	private void addGeos(ArrayList<RouteStep> stepList2) {
		// TODO Auto-generated method stub
		for (int i = 0; i < stepList2.size(); i++) {
			geofenceList.add(new Geofence.Builder()
					// Set the request ID of the geofence. This is a string to
					// identify this
					// geofence.
					.setRequestId("GEO" + (i + 1))

					.setCircularRegion(stepList2.get(i).getStart().latitude,
							stepList2.get(i).getStart().longitude, 50)
					.setExpirationDuration(24 * 60 * 60 * 1000)
					.setTransitionTypes(
							Geofence.GEOFENCE_TRANSITION_ENTER
									| Geofence.GEOFENCE_TRANSITION_EXIT)
					.build());
		}
		geofenceList.add(new Geofence.Builder()
				// Set the request ID of the geofence. This is a string to
				// identify this
				// geofence.
				.setRequestId("GEO" + (stepList2.size() + 1))

				.setCircularRegion(
						stepList2.get(stepList2.size() - 1).getEnd().latitude,
						stepList2.get(stepList2.size() - 1).getEnd().longitude,
						50)
				.setExpirationDuration(24 * 60 * 60 * 1000)
				.setTransitionTypes(
						Geofence.GEOFENCE_TRANSITION_ENTER
								| Geofence.GEOFENCE_TRANSITION_EXIT).build());

	}

	private GeofencingRequest getGeofencingRequest() {
		GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
		builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
		builder.addGeofences(geofenceList);
		return builder.build();
	}

	private PendingIntent getGeofencePendingIntent() {
		// Reuse the PendingIntent if we already have it.
		if (mGeofencePendingIntent != null) {
			return mGeofencePendingIntent;
		}
		Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
		// We use FLAG_UPDATE_CURRENT so that we get the same pending intent
		// back when
		// calling addGeofences() and removeGeofences().
		return PendingIntent.getService(this, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
	}

	public String getStringFromDocument(Document doc) {
		try {
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(domSource, result);
			return writer.toString();
		} catch (TransformerException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			mGravity = event.values;
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			mGeomagnetic = event.values;

		if (mGravity != null && mGeomagnetic != null) {
			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, mGravity,
					mGeomagnetic);
			if (success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);
				double lastSensorChangeAngle = Math.toDegrees(orientation[0]);
				if (currentMarker != null)
					currentMarker.setRotation((float) lastSensorChangeAngle);
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	public class SuggestionsAsync extends
			AsyncTask<String, Integer, List<Address>> {

		@Override
		protected List<Address> doInBackground(String... params) {
			// TODO Auto-generated method stub
			String text = params[0];
			List<Address> resultList;
			try {
				resultList = gc.getFromLocationName(text + "", 100);
				return resultList;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnected(Bundle arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnectionSuspended(int arg0) {
		// TODO Auto-generated method stub
		mGoogleApiClient.connect();
	}

	@Override
	public void onResult(Status status) {
		// TODO Auto-generated method stub
		
		if(status.isSuccess())
		{
			Toast.makeText(this, "Geofences added!", Toast.LENGTH_SHORT).show();
		}
		else
		{
			Toast.makeText(this, status.getStatusCode()+" code", Toast.LENGTH_SHORT).show();
		}
		
	}
	

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }
    
    
    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

}
