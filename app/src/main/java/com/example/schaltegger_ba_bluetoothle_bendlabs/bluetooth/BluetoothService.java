package com.example.schaltegger_ba_bluetoothle_bendlabs.bluetooth;

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

import com.example.schaltegger_ba_bluetoothle_bendlabs.MainActivity;
import com.example.schaltegger_ba_bluetoothle_bendlabs.R;
import com.example.schaltegger_ba_bluetoothle_bendlabs.angle.AnglePair;
import com.example.schaltegger_ba_bluetoothle_bendlabs.angle.AngleObservable;

import java.util.Objects;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.bluetooth.SensorCommunicator.ACTION_ANGLE_DATA_AVAILABLE;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.bluetooth.SensorCommunicator.EXTRA_ANGLE_0;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.bluetooth.SensorCommunicator.EXTRA_ANGLE_1;

public class BluetoothService extends Service {
    private ServiceHandler serviceHandler;
    private final IBinder binder = new LocalBinder();
    private AngleObservable angleObservable = AngleObservable.getInstance();

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            assert device != null;
            if (device.getAddress().equals(MainActivity.IDOFSensor)) {
                connect(device.getAddress());
            }
        }
    };

    public void discover() {
        registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        sendToThread("discover");
    }
    // receives Broadcasts
    final BroadcastReceiver blEReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            if (ACTION_ANGLE_DATA_AVAILABLE.equals(action)) {
                processAngleData(intent.getFloatExtra(EXTRA_ANGLE_0, 0), intent.getFloatExtra(EXTRA_ANGLE_1, 0));
            }
        }
    };


    private void processAngleData(float angleX, float angleY) {
        angleObservable.onNext(new AnglePair(angleX,angleY));
    }

    public BluetoothService() {

    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        private String TAG = "Thread";
        private BluetoothAdapter mBTAdapter;

        ServiceHandler(Looper looper) {
            super(looper);
            mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio
            if (!mBTAdapter.cancelDiscovery()) {
                Log.e(TAG, "Discovery could not be stopped with state " + (mBTAdapter.getState() == BluetoothAdapter.STATE_ON));
            } else {
                Log.i(TAG, "Discovery stopped");
            }
        }

        @Override
        public void handleMessage(Message msg) {
            switch (Objects.requireNonNull(msg.getData().getString("ACTION"))) {
                case "connect":
                    String address = msg.getData().getString("address");
                    Log.i("Thread", "Connecting to: " + address);
                    new SensorCommunicator(mBTAdapter.getRemoteDevice(address), BluetoothService.this);
                    break;
                case "discover":
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

    public void sendToThread(String action) {
        Message m = new Message();
        Bundle b = new Bundle();
        b.putString("ACTION", action);
        m.setData(b);
        serviceHandler.sendMessage(m);
    }


    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("AngleThread",
                THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        Looper serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
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
