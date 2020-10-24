package com.example.schaltegger_ba_bluetoothle_bendlabs;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.example.schaltegger_ba_bluetoothle_bendlabs.finger.DISPLAYFINGER;
import com.example.schaltegger_ba_bluetoothle_bendlabs.finger.IDisplayFingerObserver;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.joniras.anglesensor.angle.AnglePair;
import com.joniras.anglesensor.angle.AngleSensor;
import com.joniras.anglesensor.angle.interfaces.AngleReceiver;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, IDisplayFingerObserver, AngleReceiver {

    private GoogleMap mMap;
    private DISPLAYFINGER currentFinger = DISPLAYFINGER.OFF;
    private float currentZoom = 10;
    private static final float AngleChangeForMove = 30;
    private static final double zoomFactor = 0.05;
    private SupportMapFragment mapFragment;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        AngleSensor.getInstance().registerReceiver(50, this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        ((MyLayout)findViewById(R.id.mylayout)).registerObserver(this);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        ((MyLayout)findViewById(R.id.mylayout)).setMap(mapFragment.getView());
        mMap.moveCamera(CameraUpdateFactory.zoomTo(currentZoom));
        getDeviceLocation();


        mMap.setOnCameraMoveListener(() -> {
            float zoom = mMap.getCameraPosition().zoom;
            if (zoom != currentZoom){
                currentZoom = zoom;
            }
        });
    }

    private void getDeviceLocation() {
        try {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Set the map's camera position to the current location of the device.
                        Location lastKnownLocation = task.getResult();
                        if (lastKnownLocation != null) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(lastKnownLocation.getLatitude(),
                                            lastKnownLocation.getLongitude()), currentZoom));
                        }
                    }
                });
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AngleSensor.getInstance().unregisterReceiver(this);
        ((MyLayout)findViewById(R.id.mylayout)).removeObserver(this);
    }

    @Override
    public void onFingerOnDisplayChanged(DISPLAYFINGER fingerOnDisplay) {
        this.currentFinger = fingerOnDisplay;
    }

    @Override
    public void processAngleDataMillis(AnglePair angle) {
        if (angle.getY() > AngleChangeForMove) {
            if (currentFinger == DISPLAYFINGER.OFF) {
                currentZoom += zoomFactor;
            } else {
                currentZoom -= zoomFactor;
            }
            mMap.moveCamera(CameraUpdateFactory.zoomTo(currentZoom));
        }
    }
}
