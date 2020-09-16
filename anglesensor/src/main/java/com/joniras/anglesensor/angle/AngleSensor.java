package com.joniras.anglesensor.angle;

import android.Manifest;
import android.app.Activity;
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

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.joniras.anglesensor.angle.interfaces.IAngleObserver;
import com.joniras.anglesensor.angle.interfaces.IAngleSensorObserver;

import java.util.ArrayList;

import static com.joniras.anglesensor.angle.SensorCommunicator.ACTION_BATTERY_DATA_AVAILABLE;
import static com.joniras.anglesensor.angle.SensorCommunicator.ACTION_GATT_CONNECTED;
import static com.joniras.anglesensor.angle.SensorCommunicator.ACTION_GATT_DISCONNECTED;
import static com.joniras.anglesensor.angle.SensorCommunicator.ACTION_GATT_SERVICES_DISCOVERED;
import static com.joniras.anglesensor.angle.SensorCommunicator.EXTRA_BATTERY;

public class AngleSensor {
    private static final String TAG = "AngleSensor";

    private static final AngleSensor instance = new AngleSensor();

    private static BluetoothService service;

    private static ArrayList<IAngleSensorObserver> angleSensorObservers = new ArrayList<>();
    private static ArrayList<IAngleObserver> angleObservers = new ArrayList<>();


    public static AngleSensor getInstance() {
        return instance;
    }

    /**
     * The Sensor from the AAU with the Inventory number "ISYS-2019-122"
     */
    private String IDOFSensor = "FA:0E:BA:83:09:8A";

    /**
     * @param id The Hardware-ID as String that should be connected to (defaults to Hardware-ID of Sensor which originally was used)
     */
    public void setIDOfSensor(String id){
        IDOFSensor = id;
    }

    public String getIDOFSensor() {
        return IDOFSensor;
    }

    /**
     * Initialise the Bluetooth Service and check for permissions
     * @param context for the BluetoothService
     */
    public void start(Activity context) {
        // start the Bluetooth service and bind it to the Activity
        Intent intent = new Intent(context, BluetoothService.class);
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);

        // register the Receivver for the Broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(ACTION_BATTERY_DATA_AVAILABLE);
        filter.addAction(ACTION_GATT_CONNECTED);
        filter.addAction(ACTION_GATT_DISCONNECTED);
        filter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        context.registerReceiver(blEReceiver, filter);

        // Ask for location permission if not already allowed
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

    }

    /**
     * Starts to discover Bluetooth Devices and connects to the IDOFSensor if it is in reach
     * @throws Exception if service not ready (probably a problem with permission for bluetooth)
     */
    public void discover() throws Exception {
        // If service is null probably something with permissions is wrong
        if(service != null){
            service.discover();
        }else{
            throw new Exception("Service not ready (Permission failure)");
        }
    }


    /**
     * Register with this function if you want Angle Data and General Data (Batter, etc)
     * @param observer
     */
    public void registerObserver(IAngleSensorObserver observer) {
        if(!angleSensorObservers.contains(observer)) {
            angleSensorObservers.add(observer);
        }
    }


    /**
     * Removes the observer from the notification list
     * @param observer
     */
    public void removeObserver(IAngleSensorObserver observer) {
        angleSensorObservers.remove(observer);
    }

    /**
     * Register with this function if you want only Angle Data
     * @param observer
     */
    public void registerObserver(IAngleObserver observer) {
        if(!angleObservers.contains(observer)) {
            angleObservers.add(observer);
        }
    }


    /**
     * Removes the observer from the notification list
     * @param observer
     */
    public void removeObserver(IAngleObserver observer) {
        angleObservers.remove(observer);
    }

    /**
     * Object that receives the Broadcasts from the BluetoothAdapter (State changes) and the SensorCommunicator (Data changes)
     * forwards the events to the observer
     */
   private final BroadcastReceiver blEReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            switch (action) {
                // If Bluetooth is On Or Of
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
                    notifyBatteryChange(intent.getIntExtra(EXTRA_BATTERY, 0));
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

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.i(TAG,"Service connected");
            // Bind Service to Activity
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            AngleSensor.service = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(TAG,"Service disconncted");
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.i(TAG,"Service binding died");
        }

        @Override
        public void onNullBinding(ComponentName name) {
            Log.i(TAG,"Service NULL binding");
        }

    };

    /**
     * Notify all Observer about Battery Status
     * @param percent
     */
    private void notifyBatteryChange(int percent) {
        for (IAngleSensorObserver observer: angleSensorObservers) {
            observer.onBatteryChange(percent);
        }
    }

    /**
     * Notify all Observer when a device has connected
     */
    private void notifyDeviceConnected(){
        for (IAngleSensorObserver observer: angleSensorObservers) {
            observer.onDeviceConnected();
        }
    }

    /**
     * Notify all Observer when a device has disconnected
     */
    private void notifyDeviceDisconnected(){
        for (IAngleSensorObserver observer: angleSensorObservers) {
            observer.onDeviceDisconnected();
        }
    }

    /**
     * Notify all Observer when Bluetooth State changed (on or off)
     * @param isOn
     */
    private void notifyBluetoothStateChanged(boolean isOn){
        for (IAngleSensorObserver observer: angleSensorObservers) {
            observer.onBluetoothStateChanged(isOn);
        }
    }

    /**
     * Notify all Observer about new AngleData
     * @param anglePair
     */
    void notifyAngleDataChanged(AnglePair anglePair){
        for (IAngleSensorObserver observer: angleSensorObservers) {
            observer.onAngleDataChanged(anglePair);
        }
        for (IAngleObserver observer: angleObservers) {
            observer.onAngleDataChanged(anglePair);
        }
    }

}
