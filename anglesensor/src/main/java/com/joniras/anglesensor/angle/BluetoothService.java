package com.joniras.anglesensor.angle;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.joniras.anglesensor.R;

import java.util.Objects;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

/**
 * Service, der mit der SensorCommunicator Objekt kommuniziert
 */
public class BluetoothService extends Service {
    private ServiceHandler serviceHandler;
    private final IBinder binder = new LocalBinder();
    private AngleSensor angleSensor = AngleSensor.getInstance();

    private boolean found = false;

    public static final String ACTION_DISCOVERY_TIMEOUT = "aau.sensor_evaluation.ACTION_DISCOVERY_TIMEOUT";

    /**
     * Empfängt den Broadcast bei Änderung des Bleutooth-Verbindungs oder Suchen-Zustands
     * Initialisiert Verbindung zum Sensor, wenn dieser gefunden wurde
     */
    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                // Ein Bluetooth-Gerät wurde gefunden
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if ( device != null && device.getAddress().equals(angleSensor.getIDOFSensor())) {
                        found = true;
                        connect(device.getAddress());
                    }
                    break;
                // Das Suchen nach neuen Geräten hat aufgehört
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    if(!found){
                        sendBroadcast(new Intent(ACTION_DISCOVERY_TIMEOUT));
                    }
                    break;
            }
        }
    };

    /**
     * Startet den Suchvorgang nach neuen Geräten
     * @param initialAngle gibt an, ob Winkel-Daten direkt nach entstandener Verbindung abonniert werden sollen
     */
    public void discover(boolean initialAngle) {
        found = false;
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(blReceiver, filter);
        sendToThread("discover", "initialAngle",initialAngle);
    }

    public BluetoothService() {}

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        private String TAG = "Thread";
        private BluetoothAdapter mBTAdapter;
        private boolean initialAngle = false;

        ServiceHandler(Looper looper) {
            super(looper);
            mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio
            if (!mBTAdapter.cancelDiscovery()) {
                Log.e(TAG, "Discovery could not be stopped with state " + (mBTAdapter.getState()));
            } else {
                Log.v(TAG, "Discovery stopped");
            }
        }

        /**
         * Funktion leitet Nachrichten vom
         * @param msg that comes from the Service to the Thread
         */
        @Override
        public void handleMessage(Message msg) {
            switch (Objects.requireNonNull(msg.getData().getString("ACTION"))) {
                case "connect":
                    String address = msg.getData().getString("address");
                    Log.d("Thread", "Connecting to: " + address);
                    //launch the SensorCommunicator who is responsible for the communication to the Sensor
                    SensorCommunicator.getInstance().connect(mBTAdapter.getRemoteDevice(address), BluetoothService.this, initialAngle);
                    break;
                case "discover":
                    initialAngle = msg.getData().getBoolean("initialAngle");
                    if (mBTAdapter.isDiscovering()) {
                        mBTAdapter.cancelDiscovery();
                        Toast.makeText(getApplicationContext(), R.string.bl_discovery_stopped, Toast.LENGTH_SHORT).show();
                    } else {
                        if (mBTAdapter.isEnabled()) {
                            Toast.makeText(getApplicationContext(), R.string.bl_discovery_started, Toast.LENGTH_SHORT).show();
                            if (!mBTAdapter.startDiscovery()) {
                                Log.e(TAG, "Discovery could not be started");
                            } else {
                                Log.d(TAG, "Discovery started");
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.bl_not_on, Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case "rate":
                    int rate = msg.getData().getInt("rate");
                    SensorCommunicator.getInstance().writeSampleRate(rate);
                    break;
                case "calibrate":
                    SensorCommunicator.getInstance().calibrate();
                    break;
                case "reset":
                    SensorCommunicator.getInstance().resetSensor();
                    break;
                case "softwarereset":
                    SensorCommunicator.getInstance().softwareResetSensor();
                    break;
                case "disconnect":
                    SensorCommunicator.getInstance().disconnect();
                    break;
                case "turnOn":
                    SensorCommunicator.getInstance().turnOnNotifications();
                    break;
                case "turnOff":
                    SensorCommunicator.getInstance().turnOffNotifications();
                    break;
                case "readSensorInformation":
                    SensorCommunicator.getInstance().readSensorInformation();
                    break;
            }
        }
    }

    public void connect(String address) {
        sendToThread("connect", "address", address);
    }

    public void sendToThread(String action, String paramName, String param) {
        Message m = new Message();
        Bundle b = new Bundle();
        b.putString(paramName, param);
        b.putString("ACTION", action);
        m.setData(b);
        serviceHandler.sendMessage(m);
    }

    public void sendToThread(String action, String paramName, boolean param) {
        Message m = new Message();
        Bundle b = new Bundle();
        b.putBoolean(paramName, param);
        b.putString("ACTION", action);
        m.setData(b);
        serviceHandler.sendMessage(m);
    }

    public void sendToThread(String action, String paramName, int param) {
        Message m = new Message();
        Bundle b = new Bundle();
        b.putInt(paramName, param);
        b.putString("ACTION", action);
        m.setData(b);
        serviceHandler.sendMessage(m);
    }

    public void sendToThread(String action) {
        Message m = new Message();
        Bundle b = new Bundle();
        b.putString("ACTION", action);
        m.setData(b);
        serviceHandler.sendMessage(m);
    }

    /**
     * Function gets called when the Service is started
     *
     */
    @Override
    public void onCreate() {
        //start Thread that Handles the communication with the Sensor (Service gets maybe killed when switchgin activities)
        HandlerThread thread = new HandlerThread("AngleThread", THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // the handler is used to communicate with the Thread
        serviceHandler = new ServiceHandler(thread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Telling the system that we start and stop by ourselves
        return START_STICKY;
    }

    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BluetoothService.this;
        }

    }


    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        return binder;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service died", Toast.LENGTH_SHORT).show();
        unregisterReceiver(blReceiver);
    }
}
