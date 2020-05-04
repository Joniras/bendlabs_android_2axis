package com.example.schaltegger_ba_bluetoothle_bendlabs;

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
import android.view.View;
import android.widget.Toast;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.BluetoothLEService.ACTION_ANGLE_DATA_AVAILABLE;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.BluetoothLEService.ACTION_BATTERY_DATA_AVAILABLE;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.BluetoothLEService.ACTION_GATT_CONNECTED;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.BluetoothLEService.ACTION_GATT_DISCONNECTED;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.BluetoothLEService.EXTRA_ANGLE_0;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.BluetoothLEService.EXTRA_ANGLE_1;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.BluetoothLEService.EXTRA_DATA;

public class AngleService extends Service {
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private final IBinder binder = new LocalBinder();
    private CompositeDisposable disposable = new CompositeDisposable();
    private PublishSubject<AnglePair> _angle = PublishSubject.create();
    private AngleData angleData = AngleData.getInstance();

    public void discover() {
        sendToThread("discover");
    }
    // receives Broadcasts
    final BroadcastReceiver blEReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            switch (action) {
                case ACTION_ANGLE_DATA_AVAILABLE:
                    processAngleData(intent.getFloatExtra(EXTRA_ANGLE_0, 0), intent.getFloatExtra(EXTRA_ANGLE_1, 0));
                    break;
            }
        }
    };


    private void processAngleData(float angleX, float angleY) {
        angleData.onNext(new AnglePair(angleX,angleY));
    }

    public AngleService() {

    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        private String TAG = "Thread";
        private BluetoothAdapter mBTAdapter;

        public ServiceHandler(Looper looper) {
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
            switch (msg.getData().getString("ACTION")) {
                case "connect":
                    String address = msg.getData().getString("address");
                    Log.i("Thread", "Connecting to: " + address);
                    new BluetoothLEService(mBTAdapter.getRemoteDevice(address), AngleService.this);
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
        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work doesn't disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
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

    class LocalBinder extends Binder {
        AngleService getService() {
            // Return this instance of LocalService so clients can call public methods
            return AngleService.this;
        }

        void subscribeWith(DisposableObserver<AnglePair> e) {
            disposable.add(_angle.subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).subscribeWith(e));
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        return binder;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }
}
