package com.joniras.anglesensor.angle;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.joniras.anglesensor.angle.interfaces.IAngleDataReceiver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.joniras.anglesensor.angle.SensorCommunicator.ACTION_ANGLE_DATA_AVAILABLE;
import static com.joniras.anglesensor.angle.SensorCommunicator.EXTRA_ANGLE_X;
import static com.joniras.anglesensor.angle.SensorCommunicator.EXTRA_ANGLE_Y;

/**
 * Service, der mit der SensorCommunicator Objekt kommuniziert
 */
 class BluetoothService extends Service {
    private String TAG = "Service";
    private final IBinder binder = new LocalBinder();
    private BluetoothAdapter mBTAdapter;
    private boolean initialAngle = false;
    private boolean found = false;

    // Zeit, nach der aufgehört wird nach dem Sensor zu suchen
    private final static int bleDiscoveryTimeout = 5000;

    private List<AngleReceiverObject> angleReceiverObjectList = new ArrayList<>();

    private Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBTAdapter.cancelDiscovery()) {
            Log.e(TAG, "Discovery could not be stopped with state " + (mBTAdapter.getState()));
        } else {
            Log.v(TAG, "Discovery stopped");
        }
    }

    static final String ACTION_DISCOVERY_TIMEOUT = "aau.sensor_evaluation.ACTION_DISCOVERY_TIMEOUT";

    /**
     * Empfängt den Broadcast bei Änderung des Bluetooth-Verbindungs oder Suchen-Zustands
     * Initialisiert Verbindung zum Sensor, wenn dieser gefunden wurde
     */
    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            assert intent.getAction() != null;
            switch (intent.getAction()) {
                case ACTION_ANGLE_DATA_AVAILABLE:
                    notifyReceiver(new AnglePair(intent.getFloatExtra(EXTRA_ANGLE_X, 0), intent.getFloatExtra(EXTRA_ANGLE_Y, 0)));
            }
        }
    };

    /**
     * Startet den Suchvorgang nach neuen Geräten
     *
     * @param initialAngle gibt an, ob Winkel-Daten direkt nach entstandener Verbindung abonniert werden sollen
     */
    public void discover(boolean initialAngle) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_ANGLE_DATA_AVAILABLE);
        registerReceiver(blReceiver, filter);
        this.initialAngle = initialAngle;
        if (mBTAdapter.isEnabled()) {
            handler.removeCallbacksAndMessages(null);
            found = false;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBTAdapter.getBluetoothLeScanner().stopScan(bleScanCallback);
                    if (!found) {
                        sendBroadcast(new Intent(ACTION_DISCOVERY_TIMEOUT));
                    }
                }
            }, bleDiscoveryTimeout);
            mBTAdapter.getBluetoothLeScanner().startScan(bleScanCallback);
        }
    }

    private ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            String address = result.getDevice().getAddress();
            if(address.equals(AngleSensor.getInstance().getIDOFSensor())){
                found = true;
                connect(address);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            sendBroadcast(new Intent(ACTION_DISCOVERY_TIMEOUT));
        }
    };


    private void notifyReceiver(AnglePair anglePair) {
        long current_time = Calendar.getInstance().getTimeInMillis();
        for (AngleReceiverObject angleReceiverObject : angleReceiverObjectList) {
            if (angleReceiverObject != null) {
                // Synchronized notwendig da ansonsten Empfänger teilweise mehrmals benachrichtigt werden (da Winkeldaten schnell ankommen)
                synchronized (this) {
                    if (current_time - angleReceiverObject.getLast_update() > angleReceiverObject.getUpdate_every()) {
                        angleReceiverObject.setLast_update(current_time);
                        angleReceiverObject.getAngleReceiver().processAngleDataMillis(anglePair);
                    }
                }
            }
        }
    }


    public void registerReceiver(long update_every, IAngleDataReceiver angleReceiver) {
        boolean found = false;
        for (AngleReceiverObject angleReceiverObject : angleReceiverObjectList) {
            if (angleReceiverObject.getAngleReceiver().equals(angleReceiver)) {
                found = true;
                break;
            }
        }
        if (!found) {
            angleReceiverObjectList.add(new AngleReceiverObject(update_every, angleReceiver));
        }
    }

    public void unregisterReceiver(IAngleDataReceiver angleReceiver) {
        AngleReceiverObject toDelete = null;
        for (AngleReceiverObject angleReceiverObject : angleReceiverObjectList) {
            if (angleReceiverObject.getAngleReceiver() == angleReceiver) {
                toDelete = angleReceiverObject;
            }
        }
        if (toDelete != null) {
            angleReceiverObjectList.remove(toDelete);
        }
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

    public void connect(String address) {
        Log.d(TAG, "Connecting to: " + address);
        //launch the SensorCommunicator who is responsible for the communication to the Sensor
        SensorCommunicator.getInstance().connect(mBTAdapter.getRemoteDevice(address), BluetoothService.this, initialAngle);
    }

    public void setRate(int rate) {
        SensorCommunicator.getInstance().writeSampleRate(rate);
    }

    public void calibrate() {
        SensorCommunicator.getInstance().calibrate();
    }

    public void resetCalibration() {
        SensorCommunicator.getInstance().resetSensor();
    }

    public void resetSoftware() {
        SensorCommunicator.getInstance().softwareResetSensor();
    }

    public void disconnect() {
        SensorCommunicator.getInstance().disconnect();
    }

    public void turnOn() {
        SensorCommunicator.getInstance().turnOnNotifications();
    }

    public void turnOff() {
        SensorCommunicator.getInstance().turnOffNotifications();
    }

    public void readSensorInformation() {
        SensorCommunicator.getInstance().readSensorInformation();
    }


}
