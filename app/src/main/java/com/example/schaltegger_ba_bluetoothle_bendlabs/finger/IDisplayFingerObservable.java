package com.example.schaltegger_ba_bluetoothle_bendlabs.finger;

public interface IDisplayFingerObservable {
    void registerObserver(IDisplayFingerObserver fingerObserver);
    void removeObserver(IDisplayFingerObserver fingerObserver);
    void notifyObservers();
}
