package com.joniras.anglesensor.angle;

public interface IAngleSensorObserver extends IAngleObserver{
    void onBatteryChange(int percent);
    void onDeviceConnected();
    void onDeviceDisconnected();
    void onBluetoothStateChanged(boolean isOn);
}
