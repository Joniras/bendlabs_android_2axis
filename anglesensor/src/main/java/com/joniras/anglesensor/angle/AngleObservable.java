package com.joniras.anglesensor.angle;

import java.util.ArrayList;

public class AngleObservable implements IAngleObservable {

    private static final AngleObservable instance = new AngleObservable();
    private ArrayList<IAngleObserver> mObservers;

    public static AngleObservable getInstance() {
        return instance;
    }
    private AnglePair currentAngle = null;

    private AngleObservable() {
        mObservers = new ArrayList<>();
    }


    public void onNext(AnglePair anglePair) {
        currentAngle = anglePair;
        notifyObservers();
    }

    @Override
    public void registerObserver(IAngleObserver angleObserver) {
        if(!mObservers.contains(angleObserver)) {
            mObservers.add(angleObserver);
        }
    }

    @Override
    public void removeObserver(IAngleObserver angleObserver) {
        mObservers.remove(angleObserver);
    }

    @Override
    public void notifyObservers() {
        for (IAngleObserver observer: mObservers) {
            observer.onAngleDataChanged(currentAngle);
        }
    }
}
