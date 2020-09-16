package com.joniras.anglesensor.angle.interfaces;

import com.joniras.anglesensor.angle.AnglePair;

public interface IAngleSensorObserver {
    void onBatteryChange(int percent);
    void onDeviceConnected();
    void onDeviceDisconnected();
    void onBluetoothStateChanged(boolean isOn);
    void onAngleDataChanged(AnglePair a);
}
