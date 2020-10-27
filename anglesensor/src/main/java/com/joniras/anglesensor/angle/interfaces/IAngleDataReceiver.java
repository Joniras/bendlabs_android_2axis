package com.joniras.anglesensor.angle.interfaces;

import com.joniras.anglesensor.angle.AnglePair;

/**
 * Interface für Klassen, die Winkeldaten in einem bestimmten Zeitabstand bekommen wollen <br>
 * Registrierung über die Methode {@link com.joniras.anglesensor.angle.AngleSensor#registerReceiver(long, IAngleDataReceiver)}
 */
public interface IAngleDataReceiver {
    /**
     * Funktion wird in den angeforderten Zeitabständen aufgerufen wenn Winkeldaten vorhanden sind
     * @param anglePair das neueste Winkelpaar
     */
    void processAngleDataMillis(AnglePair anglePair);
}
