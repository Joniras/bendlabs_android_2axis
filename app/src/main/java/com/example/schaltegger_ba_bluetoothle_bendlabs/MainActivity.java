package com.example.schaltegger_ba_bluetoothle_bendlabs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.joniras.anglesensor.angle.AnglePair;
import com.joniras.anglesensor.angle.AngleSensor;
import com.joniras.anglesensor.angle.SensorInformation;
import com.joniras.anglesensor.angle.interfaces.ISensorDataObserver;

import java.util.Locale;


public class MainActivity extends Activity implements View.OnClickListener, ISensorDataObserver, SeekBar.OnSeekBarChangeListener, TextView.OnEditorActionListener {

    // GUI Components
    private TextView mBluetoothStatus;
    private TextView angle_x;
    private TextView angle_y;
    private LinearLayout angleResult;

    private final String TAG = MainActivity.class.getSimpleName();
    private AngleSensor angleSensor = AngleSensor.getInstance();
    private boolean initialAngle = true;
    private int sampleRate = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        angleSensor.start(this);
        angleSensor.registerObserver(this);

        setContentView(R.layout.activity_main);
        ((SeekBar)findViewById(R.id.initialAngleSlider)).setOnSeekBarChangeListener(this);
        ((EditText)findViewById(R.id.sampleRate)).setOnEditorActionListener(this);
        mBluetoothStatus = findViewById(R.id.bluetoothStatus);
        angle_x = findViewById(R.id.angle_x);
        angle_y = findViewById(R.id.angle_y);
        angleResult = findViewById(R.id.angleresult);
    }

    private void displayBluetoothState(boolean isOn) {
        mBluetoothStatus.setText(isOn ? getString(R.string.bl_enabled) : getString(R.string.bL_disbaled));
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.connect:
                try {
                    angleSensor.discover(initialAngle);
                    mBluetoothStatus.setText("Searching for Sensor");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_maps:
                Intent intent = new Intent(this, MapsActivity.class);
                startActivity(intent);
                break;
            case R.id.rateButton:
                int rate = Integer.parseInt(((EditText)findViewById(R.id.sampleRate)).getText().toString());
                if(rate >= 0){
                    try {
                        angleSensor.setRate(rate);
                        Toast.makeText(this, "Sample rate set", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Sample rate could not be set", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }else{
                    Toast.makeText(this, "Rate is not valid", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.reset:
                try {
                    angleSensor.resetSensor();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.calibrate:
                try {
                    angleSensor.calibrate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.resetSoftware:
                try {
                    angleSensor.resetSensorSoftware();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.disconnect:
                try {
                    angleSensor.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.turnOff:
                try {
                    angleSensor.turnOff();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.turnOn:
                try {
                    angleSensor.turnOn();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.readSensorInformation:
                try {
                    angleSensor.readSensorInformation();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.initialAngle:
                initialAngle = ((Switch)findViewById(R.id.initialAngle)).isChecked();
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
    public void onDeviceConnected() {
        mBluetoothStatus.setText(R.string.bl_connected);
        angleResult.setVisibility(View.VISIBLE);
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
    public void onSensorInformation(SensorInformation info) {
        new AlertDialog.Builder(this)
                .setTitle("Sensor Information")
                .setMessage(info.toString())
                .setNegativeButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    @Override
    public void onDeviceNotFound() {
        mBluetoothStatus.setText("Device not found");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        angleSensor.removeObserver(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(fromUser){
            this.sampleRate = progress;
            ((EditText)findViewById(R.id.sampleRate)).setText(Integer.toString(progress));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        sampleRate = Integer.parseInt(((EditText)v).getText().toString());
        ((SeekBar)findViewById(R.id.initialAngleSlider)).setProgress(sampleRate);
        return false;
    }
}
