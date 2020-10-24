package com.joniras.anglesensor.angle.interfaces;

import com.joniras.anglesensor.angle.AnglePair;

public interface IAngleReceiver {
    void processAngleDataMillis(AnglePair a);
}
