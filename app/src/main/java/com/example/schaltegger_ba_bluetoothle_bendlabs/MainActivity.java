package com.example.schaltegger_ba_bluetoothle_bendlabs;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

import static com.example.schaltegger_ba_bluetoothle_bendlabs.BluetoothLEService.ACTION_ANGLE_DATA_AVAILABLE;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.BluetoothLEService.ACTION_BATTERY_DATA_AVAILABLE;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.BluetoothLEService.ACTION_GATT_CONNECTED;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.BluetoothLEService.ACTION_GATT_DISCONNECTED;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.BluetoothLEService.EXTRA_ANGLE_0;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.BluetoothLEService.EXTRA_ANGLE_1;
import static com.example.schaltegger_ba_bluetoothle_bendlabs.BluetoothLEService.EXTRA_DATA;


public class MainActivity extends Activity implements AdapterView.OnItemClickListener, View.OnClickListener {

    // GUI Components
    private TextView mBluetoothStatus;
    private TextView angle_x;
    private TextView angle_y;
    private LinearLayout angleResult;
    private Switch blSwitch;
    private BluetoothAdapter mBTAdapter;
    private ArrayAdapter<BTDevice> mBTArrayAdapter;
    private AngleData angleData = AngleData.getInstance();

    private final String TAG = MainActivity.class.getSimpleName();

    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names


    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                assert device != null;
                mBTArrayAdapter.add(new BTDevice(device.getName(), device.getAddress(), device.getType()));
                mBTArrayAdapter.notifyDataSetChanged();
                if (device.getAddress().equals("FA:0E:BA:83:09:8A")) {
                    connect(device);
                }
            }
        }
    };

    // receives Broadcasts
    final BroadcastReceiver blEReceiver = new BroadcastReceiver() {
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
                            displayBluetoothState(false);
                            break;
                        case BluetoothAdapter.STATE_ON:
                            displayBluetoothState(true);
                            break;
                    }
                    break;
                case ACTION_ANGLE_DATA_AVAILABLE:
                    processAngleData(intent.getFloatExtra(EXTRA_ANGLE_0, 0), intent.getFloatExtra(EXTRA_ANGLE_1, 0));
                    break;
                case ACTION_BATTERY_DATA_AVAILABLE:
                    showBatteryLevel(intent.getIntExtra(EXTRA_DATA, 0));
                    break;
                case ACTION_GATT_CONNECTED:
                    mBluetoothStatus.setText(R.string.bl_connected);
                    angleResult.setVisibility(View.VISIBLE);
                    break;
                case ACTION_GATT_DISCONNECTED:
                    mBluetoothStatus.setText(R.string.bl_disconnected);
                    break;
                case ACTION_GATT_SERVICES_DISCOVERED:
                    // Log.i(TAG, Arrays.toString(new List[]{bluetoothGatt.getServices()}));
                    break;
            }
        }


    };

    private void showBatteryLevel(int intExtra) {
        Toast.makeText(this, "Battery level: "+intExtra, Toast.LENGTH_LONG).show();
    }

    private void processAngleData(float angleX, float angleY) {
        angleData.onNext(new AnglePair(angleX,angleY));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.angleData = AngleData.getInstance();
        this.angleData.subscribeWith(new DisposableObserver<AnglePair>() {

            @Override
            public void onNext(@NonNull AnglePair angles) {
                MainActivity.this.displayAngleData(angles);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                Log.i(TAG, "error");
            }

            @Override
            public void onComplete() {}
        });

        mBluetoothStatus = findViewById(R.id.bluetoothStatus);
        angle_x = findViewById(R.id.angle_x);
        angle_y = findViewById(R.id.angle_y);
        angleResult = findViewById(R.id.angleResult);
        blSwitch = findViewById(R.id.blSwitch);
        Button mDiscoverBtn = findViewById(R.id.discover);
        Button mListPairedDevicesBtn = findViewById(R.id.PairedBtn);

        mBTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio
        if (!mBTAdapter.cancelDiscovery()) {
            Log.e(TAG, "Discovery could not be stopped with state " + (mBTAdapter.getState() == BluetoothAdapter.STATE_ON));
        } else {
            Log.i(TAG, "Discovery stopped");
        }
        displayBluetoothState(mBTAdapter.getState() == BluetoothAdapter.STATE_ON);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(ACTION_ANGLE_DATA_AVAILABLE);
        filter.addAction(ACTION_BATTERY_DATA_AVAILABLE);
        filter.addAction(ACTION_GATT_CONNECTED);
        filter.addAction(ACTION_GATT_DISCONNECTED);
        filter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        registerReceiver(blEReceiver, filter);

        IntentFilter actionFoundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(blReceiver, actionFoundFilter);

        ListView mDevicesListView = findViewById(R.id.btDeviceList);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(this);

        // Ask for location permission if not already allowed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        // Ask for location permission if not already allowed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);


        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText(R.string.bl_not_found);
            Toast.makeText(getApplicationContext(), R.string.bl_device_notfound, Toast.LENGTH_SHORT).show();
        } else {
            discover();
            blSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        bluetoothOn();
                    } else {
                        bluetoothOff();
                    }
                }
            });

            mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listPairedDevices();
                }
            });

            mDiscoverBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    discover();
                }
            });
        }
    }

    private void displayAngleData(AnglePair angles) {
        angle_x.setText(String.format(Locale.GERMAN, "%.2f", angles.getX()));
        angle_y.setText(String.format(Locale.GERMAN, "%.2f", angles.getY()));
    }

    private void bluetoothOn() {
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            displayBluetoothState(true);
            Toast.makeText(getApplicationContext(), getString(R.string.bl_enabled), Toast.LENGTH_SHORT).show();
        } else {
            displayBluetoothState(true);
            Toast.makeText(getApplicationContext(), R.string.bl_already_on, Toast.LENGTH_SHORT).show();
        }
    }

    private void displayBluetoothState(boolean isOn) {
        mBluetoothStatus.setText(isOn ? getString(R.string.bl_enabled) : getString(R.string.bL_disbaled));
        blSwitch.setChecked(isOn);
    }

    // Dialog of wanting to enable bluetooth result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                displayBluetoothState(true);
            } else
                displayBluetoothState(false);
        }
    }

    private void bluetoothOff() {
        mBTAdapter.disable();
        displayBluetoothState(false);
        Toast.makeText(getApplicationContext(), R.string.bl_turned_off, Toast.LENGTH_SHORT).show();
    }

    private void discover() {
        // Check if the device is already discovering
        if (mBTAdapter.isDiscovering()) {
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(), R.string.bl_discovery_stopped, Toast.LENGTH_SHORT).show();
        } else {
            if (mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
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


    private void listPairedDevices() {
        mBTArrayAdapter.clear();
        Set<BluetoothDevice> mPairedDevices = mBTAdapter.getBondedDevices();
        if (mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(new BTDevice(device.getName(), device.getAddress(), device.getType()));
            Toast.makeText(getApplicationContext(), R.string.bl_show_paired, Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(getApplicationContext(), R.string.bl_not_on, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (!mBTAdapter.isEnabled()) {
            Toast.makeText(getBaseContext(), R.string.bl_not_on, Toast.LENGTH_SHORT).show();
            return;
        }
        connect(mBTAdapter.getRemoteDevice(this.mBTArrayAdapter.getItem(position).getAddress()));
    }

    public void connect(BluetoothDevice device) {
        mBluetoothStatus.setText(R.string.bl_connecting);

        if (device.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
            new BluetoothLEService(device, this);
        } else {
            Toast.makeText(getBaseContext(), R.string.only_ble, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(blReceiver);
    }


    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }
}
