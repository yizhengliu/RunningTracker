package com.finalcoursework.service;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
//callback methods interface
public interface ICallBack {
    void pathsChanged(ArrayList<LatLng> lastPath);
    void timeChanged(String result);
    void distanceChanged(String result);
    void stateChanged(int state);
}
