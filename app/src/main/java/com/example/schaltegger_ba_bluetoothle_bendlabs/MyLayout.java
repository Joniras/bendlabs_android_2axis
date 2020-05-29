package com.example.schaltegger_ba_bluetoothle_bendlabs;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.core.view.MotionEventCompat;

import com.example.schaltegger_ba_bluetoothle_bendlabs.finger.DISPLAYFINGER;
import com.example.schaltegger_ba_bluetoothle_bendlabs.finger.IDisplayFingerObservable;
import com.example.schaltegger_ba_bluetoothle_bendlabs.finger.IDisplayFingerObserver;

import java.util.ArrayList;


public class MyLayout extends RelativeLayout implements IDisplayFingerObservable {

    private View map;
    private ArrayList<IDisplayFingerObserver> mObservers = new ArrayList<>();
    private DISPLAYFINGER currentState = DISPLAYFINGER.OFF;

    public MyLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setMap(View m){
        this.map = m;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){

        int action = MotionEventCompat.getActionMasked(event);

        switch(action) {
            case (MotionEvent.ACTION_DOWN) :
                Log.d("MAPS","Action was DOWN");
                currentState = DISPLAYFINGER.ON;
                this.notifyObservers();
                break;
            case (MotionEvent.ACTION_UP) :
                currentState = DISPLAYFINGER.OFF;
                this.notifyObservers();
                Log.d("MAPS","Action was UP");
                break;
        }
        if(map != null){
            map.dispatchTouchEvent(event);
        }
        return true;
    }

    @Override
    public void registerObserver(IDisplayFingerObserver fingerObserver) {
        if(!mObservers.contains(fingerObserver)) {
            mObservers.add(fingerObserver);
        }
    }

    @Override
    public void removeObserver(IDisplayFingerObserver fingerObserver) {
        mObservers.remove(fingerObserver);
    }

    @Override
    public void notifyObservers() {
        for (IDisplayFingerObserver observer: mObservers) {
            observer.onFingerOnDisplayChanged(currentState);
        }
    }
}

