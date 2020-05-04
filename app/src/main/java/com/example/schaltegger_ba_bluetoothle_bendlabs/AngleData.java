package com.example.schaltegger_ba_bluetoothle_bendlabs;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

class AngleData {

    private static final AngleData instance = new AngleData();

    static AngleData getInstance() {
        return instance;
    }

    private CompositeDisposable disposable = new CompositeDisposable();
    private PublishSubject<AnglePair> _angle = PublishSubject.create();

    private AngleData() {

    }

    void subscribeWith(DisposableObserver<AnglePair> e) {
        disposable.add(_angle.subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).subscribeWith(e));
    }


    void onNext(AnglePair anglePair) {
        _angle.onNext(anglePair);
    }
}
