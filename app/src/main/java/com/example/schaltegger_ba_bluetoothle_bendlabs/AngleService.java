package com.example.schaltegger_ba_bluetoothle_bendlabs;

import java.util.ArrayList;

class AngleService implements AngleObservable {

    private static final AngleService instance = new AngleService();
    private ArrayList<AngleObserver> mObservers;

    static AngleService getInstance() {
        return instance;
    }
    private AnglePair currentAngle = null;

    private AngleService() {
        mObservers = new ArrayList<>();
    }


    void onNext(AnglePair anglePair) {
        currentAngle = anglePair;
        notifyObservers();
    }

    @Override
    public void registerObserver(AngleObserver angleObserver) {
        if(!mObservers.contains(angleObserver)) {
            mObservers.add(angleObserver);
        }
    }

    @Override
    public void removeObserver(AngleObserver angleObserver) {
        mObservers.remove(angleObserver);
    }

    @Override
    public void notifyObservers() {
        for (AngleObserver observer: mObservers) {
            observer.onAngleDataChanged(currentAngle);
        }
    }
}
