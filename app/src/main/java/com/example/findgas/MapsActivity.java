package com.example.findgas;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    LocationManager  locationManager;
    LocationListener locationListener;

    LatLng initialPosition;
    LatLng finalPosition;
    LatLng currentPosition;

    String goalAddress = "";

    Boolean done = false; // for initial set-up
    Boolean goal = false; // for reaching destination

    final double MIN_RANGE = -0.01;
    final double MAX_RANGE = 0.01;
    final double GOAL_RANGE = .0007;
    
    Marker currentMarker;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0  && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED)
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
        }
    }

    public LatLng setNewLocation(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        Random rn = new Random();
        lat = lat + MIN_RANGE + (MAX_RANGE - MIN_RANGE) * rn.nextDouble();
        rn = new Random();
        lon = lon + MIN_RANGE + (MAX_RANGE - MIN_RANGE) * rn.nextDouble();

        return new LatLng(lat, lon);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if ( !done ) {
                    initialPosition = new LatLng(location.getLatitude(), location.getLongitude());
                    finalPosition = setNewLocation(location);

                    mMap.addMarker(new MarkerOptions()
                            .position(finalPosition)
                            .title("Your goal location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                            .draggable(false));

                    mMap.moveCamera(CameraUpdateFactory.newLatLng(initialPosition));
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(14));

                    Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

                    try {
                        double nullLat;
                        double nullLng;
                        List<Address> addressList = geocoder.getFromLocation(finalPosition.latitude, finalPosition.longitude, 1);

                        if ( addressList != null && addressList.size() > 0 ) {
                            String stf = addressList.get(0).getSubThoroughfare();
                            String tf = addressList.get(0).getThoroughfare();
                            String locality = addressList.get(0).getLocality();
                            String state = addressList.get(0).getAdminArea();
                            String postalcode = addressList.get(0).getPostalCode();
                            goalAddress = stf + " " + tf + " " + locality + ", " + state + ", " + postalcode;

                            // some locations don't have an address
                            if ( goalAddress.equals("") ) {
                                nullLat = addressList.get(0).getLatitude();
                                nullLng = addressList.get(0).getLongitude();
                                goalAddress += nullLat + " " + nullLng;
                            }
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }

                    Log.i("Goal Lat: ", Double.toString(finalPosition.latitude));
                    Log.i("Goal Lng: ", Double.toString(finalPosition.longitude));
                    done = true;
                }

                if ( !goal ) {
                    Toast.makeText(MapsActivity.this, "Get to " + goalAddress, Toast.LENGTH_LONG).show();

                    if (currentMarker != null)
                    currentMarker.remove();

                    currentPosition = new LatLng(location.getLatitude(), location.getLongitude());

                    currentMarker = mMap.addMarker(new MarkerOptions().position(currentPosition).title("Your current location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).draggable(false));

                    if ( Math.abs(currentPosition.latitude - finalPosition.latitude) < GOAL_RANGE
                            && Math.abs(currentPosition.longitude - finalPosition.longitude) < GOAL_RANGE )
                        goal = true;
                }

                if ( goal ) {
                    Toast.makeText(MapsActivity.this, "You did it!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
        else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            else
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }
}
