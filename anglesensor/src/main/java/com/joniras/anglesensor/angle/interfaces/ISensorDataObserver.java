package com.joniras.anglesensor.angle.interfaces;

public interface ISensorDataObserver extends IAngleDataObserver {
    void onBatteryChange(int percent);
    void onDeviceConnected();
    void onDeviceDisconnected();
    void onBluetoothStateChanged(boolean isOn);
}
