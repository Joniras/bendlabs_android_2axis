package com.joniras.anglesensor.angle.interfaces;

import com.joniras.anglesensor.angle.AnglePair;

/**
 * Interface für Klassen, welche  vom Sensor ausschliesslich Winkel-Daten beziehen wollen
 */
public interface IAngleDataObserver {
    void onAngleDataChanged(AnglePair a);
}
