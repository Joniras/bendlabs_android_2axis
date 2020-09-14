package com.example.schaltegger_ba_bluetoothle_bendlabs.angle;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.schaltegger_ba_bluetoothle_bendlabs.MainActivity;
import com.example.schaltegger_ba_bluetoothle_bendlabs.R;
import com.example.schaltegger_ba_bluetoothle_bendlabs.bluetooth.BluetoothService;

import java.util.ArrayList;

import static com.example.schaltegger_ba_bluetoothle_bendlabs.bluetooth.SensorCommunicator.ACTION_BATTERY_DATA_AVAILABLE;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.bluetooth.SensorCommunicator.ACTION_GATT_CONNECTED;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.bluetooth.SensorCommunicator.ACTION_GATT_DISCONNECTED;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.bluetooth.SensorCommunicator.ACTION_GATT_SERVICES_DISCOVERED;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.bluetooth.SensorCommunicator.EXTRA_DATA;

public class AngleSensor {
    private static final String TAG = "AngleSensor";

    private static BluetoothService service;

    private static AngleObservable angleObservable = AngleObservable.getInstance();
    private static ArrayList<IAngleSensorObserver> mObservers = new ArrayList<>();

    public static void discover() {
        // Check if the device is already discovering
        service.discover();
    }


    // receives Broadcasts
   static final BroadcastReceiver blEReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            notifyBluetoothStateChanged(false);
                            break;
                        case BluetoothAdapter.STATE_ON:
                            notifyBluetoothStateChanged(true);
                            break;
                    }
                    break;
                case ACTION_BATTERY_DATA_AVAILABLE:
                    notifyBatteryChange(intent.getIntExtra(EXTRA_DATA, 0));
                    break;
                case ACTION_GATT_CONNECTED:
                    notifyDeviceConnected();
                    break;
                case ACTION_GATT_DISCONNECTED:
                    notifyDeviceDisconnected();
                    break;
            }
        }


    };

    public static void start(MainActivity context) {

        Intent intent = new Intent(context, BluetoothService.class);
        context.bindService(intent, AngleSensor.connection, Context.BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(ACTION_BATTERY_DATA_AVAILABLE);
        filter.addAction(ACTION_GATT_CONNECTED);
        filter.addAction(ACTION_GATT_DISCONNECTED);
        filter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        context.registerReceiver(blEReceiver, filter);

        // Ask for location permission if not already allowed
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        // Ask for location permission if not already allowed
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private static ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.i(TAG,"Service connected");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            AngleSensor.service = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(TAG,"Service disconncted");
        }
    };

    public static void registerObserver(IAngleSensorObserver observer) {
        angleObservable.registerObserver(observer);
        if(!mObservers.contains(observer)) {
            mObservers.add(observer);
        }
    }

    public static void registerObserver(IAngleObserver observer) {
        angleObservable.registerObserver(observer);

    }

    public static void removeObserver(IAngleSensorObserver observer) {
        angleObservable.removeObserver(observer);
        mObservers.remove(observer);
    }


    public static void removeObserver(IAngleObserver observer) {
        angleObservable.removeObserver(observer);
    }

    public static void notifyBatteryChange(int percent) {
        for (IAngleSensorObserver observer: mObservers) {
            observer.onBatteryChange(percent);
        }
    }

    public static void notifyDeviceConnected(){
        for (IAngleSensorObserver observer: mObservers) {
            observer.onDeviceConnected();
        }
    }
    public static void notifyDeviceDisconnected(){
        for (IAngleSensorObserver observer: mObservers) {
            observer.onDeviceDisconnected();
        }
    }
    public static void notifyBluetoothStateChanged(boolean isOn){
        for (IAngleSensorObserver observer: mObservers) {
            observer.onBluetoothStateChanged(isOn);
        }
    }

}
