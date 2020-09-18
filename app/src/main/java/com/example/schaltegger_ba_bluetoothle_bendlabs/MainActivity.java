package com.example.schaltegger_ba_bluetoothle_bendlabs;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.joniras.anglesensor.angle.AnglePair;
import com.joniras.anglesensor.angle.AngleSensor;
import com.joniras.anglesensor.angle.interfaces.ISensorDataObserver;

import java.util.Locale;


public class MainActivity extends Activity implements View.OnClickListener, ISensorDataObserver {

    // GUI Components
    private TextView mBluetoothStatus;
    private TextView angle_x;
    private TextView angle_y;
    private LinearLayout angleResult;
    private LinearLayout battery;

    private final String TAG = MainActivity.class.getSimpleName();
    private AngleSensor angleSensor = AngleSensor.getInstance();

    private void showBatteryLevel(int batteryLevel) {
        Toast.makeText(this, "Battery level: "+batteryLevel, Toast.LENGTH_LONG).show();
        ((TextView)findViewById(R.id.batterylevel)).setText(String.format(Locale.GERMAN,"%d%%", batteryLevel));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        angleSensor.start(this);
        angleSensor.registerObserver(this);

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

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.connect:
                try {
                    angleSensor.discover();
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
        angleSensor.removeObserver(this);
    }
}
