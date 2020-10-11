package com.joniras.anglesensor.angle.interfaces;

public interface ISensorDataObserver extends IAngleDataObserver {
    void onDeviceConnected();
    void onDeviceDisconnected();
    void onBluetoothStateChanged(boolean isOn);
}
