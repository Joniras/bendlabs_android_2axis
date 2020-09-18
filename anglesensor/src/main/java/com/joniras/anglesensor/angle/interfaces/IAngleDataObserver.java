package com.joniras.anglesensor.angle.interfaces;

import com.joniras.anglesensor.angle.AnglePair;

public interface IAngleDataObserver {
    void onAngleDataChanged(AnglePair a);
}
