package com.joniras.anglesensor.angle;

interface IAngleObservable {
    void registerObserver(IAngleObserver angleObserver);
    void removeObserver(IAngleObserver angleObserver);
    void notifyObservers();
}
