package br.pucrio.inf.acanhota.autosddl.pub;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

public class LocationService extends Service implements LocationListener {
	private LocationManager locationManager;
	private String provider;
	private double lat;
	private double lng;
		
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {	
		super.onCreate();

	    // Get the location manager
	    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	    // Define the criteria how to select the location provider -> use default
	    Criteria criteria = new Criteria();
	    provider = locationManager.getBestProvider(criteria, false);
	    Location location = locationManager.getLastKnownLocation(provider);

	    // Initialize the location fields
	    if (location != null) {
	      onLocationChanged(location);
	    }
	}
	
	@Override
	public void onStart(Intent intent, int startid) {
		locationManager.requestLocationUpdates(provider, 400, 1, this);
	}
	
	@Override
	public void onDestroy() {
	}
	
	@Override
	public void onLocationChanged(Location location) {
		lat = location.getLatitude();
		lng = location.getLongitude();
		
		Intent intent = new Intent("locationChanged");
		intent.putExtra("lat", lat);
		intent.putExtra("lng", lng);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}	
	
	public Double getLat() {
		return lat;
	}
	
	@Override
	public void onProviderDisabled(String arg0) {
	}

	@Override
	public void onProviderEnabled(String arg0) {
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
	}

}