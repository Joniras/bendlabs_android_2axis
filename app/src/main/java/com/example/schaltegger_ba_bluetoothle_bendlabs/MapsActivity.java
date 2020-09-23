package com.example.schaltegger_ba_bluetoothle_bendlabs;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.joniras.anglesensor.angle.AnglePair;
import com.joniras.anglesensor.angle.AngleSensor;
import com.example.schaltegger_ba_bluetoothle_bendlabs.finger.DISPLAYFINGER;
import com.example.schaltegger_ba_bluetoothle_bendlabs.finger.IDisplayFingerObserver;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.joniras.anglesensor.angle.interfaces.IAngleDataObserver;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, IDisplayFingerObserver, IAngleDataObserver {

    private GoogleMap mMap;
    private DISPLAYFINGER currentFinger = DISPLAYFINGER.OFF;
    private float currentZoom = 10;
    private static final int skipAngles = 0;
    private static final int skipAnglesAfterMove = 10;
    private static final float AngleChangeForMove = 30;
    private static final double zoomFactor = 0.1;
    private ArrayList<AnglePair> angles = new ArrayList<>();
    private int anglesToSkip = 0;
    private SupportMapFragment mapFragment;
    private FusedLocationProviderClient mFusedLocationProviderClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        ((MyLayout)findViewById(R.id.mylayout)).registerObserver(this);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        ((MyLayout)findViewById(R.id.mylayout)).setMap(mapFragment.getView());
        mMap.moveCamera(CameraUpdateFactory.zoomTo(currentZoom));
        getDeviceLocation();
        AngleSensor.getInstance().registerObserver(this);
        // register manual zoom and save it
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition pos) {
                if (pos.zoom != currentZoom){
                    currentZoom = pos.zoom;
                }
            }
        });
    }

    private void getDeviceLocation() {
        try {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            Location lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), currentZoom));
                            }
                        }
                    }
                });

        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    @Override
    public void onAngleDataChanged(AnglePair a) {
        this.angles.add(0, a);
        if (angles.size() > (skipAngles + 2)) {
            this.angles.remove(this.angles.size() - 1);
            //skip Angles here after moving the camera
            if (anglesToSkip == 0) {
                this.checkChange();
            } else {
                anglesToSkip--;
            }
        }
    }

    private void checkChange() {
        double angle = Math.abs(this.angles.get(0).getY());
        if (angle > AngleChangeForMove) {
            anglesToSkip = skipAnglesAfterMove;
            Log.i("Maps", "Change is: " + angle + " and thumb is down: " + currentFinger);

            if (currentFinger == DISPLAYFINGER.OFF) {
                currentZoom += zoomFactor;
                mMap.moveCamera(CameraUpdateFactory.zoomTo(currentZoom));
            } else {
                currentZoom -= zoomFactor;
                mMap.moveCamera(CameraUpdateFactory.zoomTo(currentZoom));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AngleSensor.getInstance().removeObserver(this);
        ((MyLayout)findViewById(R.id.mylayout)).removeObserver(this);
    }

    @Override
    public void onFingerOnDisplayChanged(DISPLAYFINGER fingerOnDisplay) {
        this.currentFinger = fingerOnDisplay;
    }
}
