package com.example.schaltegger_ba_bluetoothle_bendlabs;

public interface AngleObservable {
    void registerObserver(AngleObserver angleObserver);
    void removeObserver(AngleObserver angleObserver);
    void notifyObservers();
}
