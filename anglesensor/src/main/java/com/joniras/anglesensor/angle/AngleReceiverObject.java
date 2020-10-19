package com.joniras.anglesensor.angle;

import com.joniras.anglesensor.angle.interfaces.AngleReceiver;

class AngleReceiverObject{
    private long last_update;
    private long update_every;
    private final AngleReceiver angleReceiver;

    public AngleReceiverObject(long update_every, AngleReceiver angleReceiver) {
        this.last_update = 0;
        this.update_every = update_every;
        this.angleReceiver = angleReceiver;
    }

    public long getLast_update() {
        return last_update;
    }

    public void setLast_update(long last_update) {
        this.last_update = last_update;
    }

    public long getUpdate_every() {
        return update_every;
    }

    public AngleReceiver getAngleReceiver() {
        return angleReceiver;
    }
}
