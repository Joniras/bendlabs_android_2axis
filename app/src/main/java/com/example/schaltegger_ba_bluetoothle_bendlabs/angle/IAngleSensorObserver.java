package com.example.schaltegger_ba_bluetoothle_bendlabs.angle;

public interface IAngleSensorObserver extends IAngleObserver{
    void onBatteryChange(int percent);
    void onDeviceConnected();
    void onDeviceDisconnected();
    void onBluetoothStateChanged(boolean isOn);
}
