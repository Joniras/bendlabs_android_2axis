package com.joniras.anglesensor.angle.interfaces;

import com.joniras.anglesensor.angle.AnglePair;

public interface AngleReceiver{
    void processAngleDataMillis(AnglePair a);
}
