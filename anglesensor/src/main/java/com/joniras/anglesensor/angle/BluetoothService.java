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
import static com.joniras.anglesensor.angle.SensorCommunicator.ACTION_ANGLE_DATA_AVAILABLE;
import static com.joniras.anglesensor.angle.SensorCommunicator.EXTRA_ANGLE_X;
import static com.joniras.anglesensor.angle.SensorCommunicator.EXTRA_ANGLE_Y;

/**
 * Service that Communicates with the Bluetooth Thread
 */
public class BluetoothService extends Service {
    private ServiceHandler serviceHandler;
    private final IBinder binder = new LocalBinder();
    private AngleSensor angleSensor = AngleSensor.getInstance();

    /**
     * Receive the Broadcast when a bluetooth device was found
     * Connects to the Sensor if it was found (by ID)
     */
    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            assert device != null;
            if (device.getAddress().equals(angleSensor.getIDOFSensor())) {
                connect(device.getAddress());
            }
        }
    };

    public void discover(boolean initialAngle) {
        registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        sendToThread("discover", "initialAngle",initialAngle);
    }

    // receives Broadcasts
    final BroadcastReceiver blEReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            if (ACTION_ANGLE_DATA_AVAILABLE.equals(action)) {
                processAngleData(intent.getFloatExtra(EXTRA_ANGLE_X, 0), intent.getFloatExtra(EXTRA_ANGLE_Y, 0));
            }
        }
    };


    private void processAngleData(float angleX, float angleY) {
        angleSensor.notifyAngleDataChanged(new AnglePair(angleX,angleY));
    }

    public BluetoothService() {

    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        private String TAG = "Thread";
        private BluetoothAdapter mBTAdapter;
        private boolean initialAngle = false;

        ServiceHandler(Looper looper) {
            super(looper);
            mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio
            if (!mBTAdapter.cancelDiscovery()) {
                Log.e(TAG, "Discovery could not be stopped with state " + (mBTAdapter.getState() == BluetoothAdapter.STATE_ON));
            } else {
                Log.i(TAG, "Discovery stopped");
            }
        }

        /**
         * Function that receives all the messages from the service
         * @param msg that comes from the Service to the Thread
         */
        @Override
        public void handleMessage(Message msg) {
            switch (Objects.requireNonNull(msg.getData().getString("ACTION"))) {
                case "connect":
                    String address = msg.getData().getString("address");
                    Log.i("Thread", "Connecting to: " + address);
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
                                Log.i(TAG, "Discovery started");
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

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(ACTION_ANGLE_DATA_AVAILABLE);
        registerReceiver(blEReceiver, filter);
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
