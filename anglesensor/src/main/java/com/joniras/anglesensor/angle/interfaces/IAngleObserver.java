package com.joniras.anglesensor.angle.interfaces;

import com.joniras.anglesensor.angle.AnglePair;

public interface IAngleObserver {
    void onAngleDataChanged(AnglePair a);
}
