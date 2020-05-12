package com.example.schaltegger_ba_bluetoothle_bendlabs.angle;

public interface IAngleObservable {
    void registerObserver(IAngleObserver angleObserver);
    void removeObserver(IAngleObserver angleObserver);
    void notifyObservers();
}
