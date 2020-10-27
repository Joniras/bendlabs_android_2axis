package com.joniras.anglesensor.angle;

import java.util.Objects;

/**
 * Klasse zum erleichterten Transport und Speichern der Winkeldaten
 */
public class AnglePair {
    private float x;
    private float y;

    AnglePair(float x, float y) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnglePair anglePair = (AnglePair) o;
        return Float.compare(anglePair.x, x) == 0 &&
                Float.compare(anglePair.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
