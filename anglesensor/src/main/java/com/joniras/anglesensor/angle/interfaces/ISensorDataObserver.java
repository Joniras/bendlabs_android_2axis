package com.joniras.anglesensor.angle.interfaces;

import com.joniras.anglesensor.angle.SensorInformation;

public interface ISensorDataObserver extends IAngleDataObserver {
    void onDeviceConnected();
    void onDeviceDisconnected();
    void onBluetoothStateChanged(boolean isOn);
    void onSensorInformation(SensorInformation info);
    void onDeviceNotFound();
}
