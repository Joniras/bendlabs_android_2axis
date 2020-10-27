package com.joniras.anglesensor.angle.interfaces;

import com.joniras.anglesensor.angle.SensorInformation;

/**
 * Interface für Klassen, welche  alle Updates bezüglich des Sensors empfangen wollen <br>
 * Registrierung über die Methode {@link com.joniras.anglesensor.angle.AngleSensor#registerObserver(ISensorDataObserver)}
 */
public interface ISensorDataObserver extends IAngleDataObserver {
    void onDeviceConnected();
    void onDeviceDisconnected();
    void onBluetoothStateChanged(boolean isOn);
    void onSensorInformation(SensorInformation info);
    void onDeviceNotFound();
}
