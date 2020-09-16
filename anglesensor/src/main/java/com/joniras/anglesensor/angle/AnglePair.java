package com.joniras.anglesensor.angle;

/**
 * This class's only purpose is to contain the X and Y Values
 */
public class AnglePair {
    private float x;
    private float y;

    public AnglePair(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }


    public float getY() {
        return y;
    }

    @Override
    public String toString() {
        return "AnglePair{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
