package com.joniras.anglesensor.angle.interfaces;

import com.joniras.anglesensor.angle.AnglePair;

/**
 * Interface für Klassen, die Winkeldaten in einem bestimmten Zeitabstand bekommen wollen
 */
public interface IAngleReceiver {
    void processAngleDataMillis(AnglePair a);
}
