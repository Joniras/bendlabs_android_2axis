package com.example.schaltegger_ba_bluetoothle_bendlabs;

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
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.schaltegger_ba_bluetoothle_bendlabs.angle.AngleSensor;
import com.example.schaltegger_ba_bluetoothle_bendlabs.angle.IAngleObserver;
import com.example.schaltegger_ba_bluetoothle_bendlabs.angle.AnglePair;
import com.example.schaltegger_ba_bluetoothle_bendlabs.angle.AngleObservable;
import com.example.schaltegger_ba_bluetoothle_bendlabs.angle.IAngleSensorObserver;
import com.example.schaltegger_ba_bluetoothle_bendlabs.bluetooth.BluetoothService;

import java.util.Locale;

import static com.example.schaltegger_ba_bluetoothle_bendlabs.bluetooth.SensorCommunicator.ACTION_BATTERY_DATA_AVAILABLE;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.bluetooth.SensorCommunicator.ACTION_GATT_CONNECTED;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.bluetooth.SensorCommunicator.ACTION_GATT_DISCONNECTED;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.bluetooth.SensorCommunicator.ACTION_GATT_SERVICES_DISCOVERED;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.bluetooth.SensorCommunicator.EXTRA_DATA;


public class MainActivity extends Activity implements View.OnClickListener, IAngleObserver, IAngleSensorObserver {

    // GUI Components
    private TextView mBluetoothStatus;
    private TextView angle_x;
    private TextView angle_y;
    private LinearLayout angleResult;
    private LinearLayout battery;
    public final static String IDOFSensor = "FA:0E:BA:83:09:8A";

    private final String TAG = MainActivity.class.getSimpleName();


    private void showBatteryLevel(int batteryLevel) {
        Toast.makeText(this, "Battery level: "+batteryLevel, Toast.LENGTH_LONG).show();
        ((TextView)findViewById(R.id.batterylevel)).setText(String.format(Locale.GERMAN,"%d%%", batteryLevel));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AngleSensor.start(this);
        AngleSensor.registerObserver(this);

        setContentView(R.layout.activity_main);

        mBluetoothStatus = findViewById(R.id.bluetoothStatus);
        angle_x = findViewById(R.id.angle_x);
        angle_y = findViewById(R.id.angle_y);
        angleResult = findViewById(R.id.angleresult);
        battery = findViewById(R.id.battery);

        // displayBluetoothState(mBTAdapter.getState() == BluetoothAdapter.STATE_ON);
    }

    private void displayBluetoothState(boolean isOn) {
        mBluetoothStatus.setText(isOn ? getString(R.string.bl_enabled) : getString(R.string.bL_disbaled));
    }

    // Dialog of wanting to enable bluetooth result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                displayBluetoothState(true);
            } else
                displayBluetoothState(false);
        }
    }



    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.connect:
                AngleSensor.discover();
                break;
            case R.id.btn_maps:
                Intent intent = new Intent(this, MapsActivity.class);
                startActivity(intent);
                break;
        }

    }


    private void displayAngleData(AnglePair angles) {
        angle_x.setText(String.format(Locale.GERMAN, "%.2f", angles.getX()));
        angle_y.setText(String.format(Locale.GERMAN, "%.2f", angles.getY()));
    }

    @Override
    public void onAngleDataChanged(AnglePair a) {
        this.displayAngleData(a);
    }

    @Override
    public void onBatteryChange(int percent) {
        showBatteryLevel(percent);
    }

    @Override
    public void onDeviceConnected() {
        mBluetoothStatus.setText(R.string.bl_connected);
        angleResult.setVisibility(View.VISIBLE);
        battery.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDeviceDisconnected() {
        mBluetoothStatus.setText(R.string.bl_disconnected);
    }

    @Override
    public void onBluetoothStateChanged(boolean isOn) {
        displayBluetoothState(isOn);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        AngleSensor.removeObserver(this);
    }
}
