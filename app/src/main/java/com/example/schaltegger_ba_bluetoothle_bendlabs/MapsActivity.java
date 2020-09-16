package com.example.schaltegger_ba_bluetoothle_bendlabs;

import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.joniras.anglesensor.angle.AnglePair;
import com.joniras.anglesensor.angle.AngleSensor;
import com.example.schaltegger_ba_bluetoothle_bendlabs.finger.DISPLAYFINGER;
import com.example.schaltegger_ba_bluetoothle_bendlabs.finger.IDisplayFingerObserver;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.joniras.anglesensor.angle.interfaces.IAngleObserver;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, IDisplayFingerObserver, IAngleObserver {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        ((MyLayout)findViewById(R.id.mylayout)).registerObserver(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        ((MyLayout)findViewById(R.id.mylayout)).setMap(mapFragment.getView());
        mMap.moveCamera(CameraUpdateFactory.zoomTo(currentZoom));
        AngleSensor.getInstance().registerObserver(this);
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
