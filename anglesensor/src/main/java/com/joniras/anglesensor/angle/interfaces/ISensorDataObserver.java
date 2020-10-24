package com.joniras.anglesensor.angle.interfaces;

import com.joniras.anglesensor.angle.SensorInformation;

/**
 * Interface für Klassen, welche  alle Updates bezüglich des Sensors empfangen wollen
 */
public interface ISensorDataObserver extends IAngleDataObserver {
    void onDeviceConnected();
    void onDeviceDisconnected();
    void onBluetoothStateChanged(boolean isOn);
    void onSensorInformation(SensorInformation info);
    void onDeviceNotFound();
}
