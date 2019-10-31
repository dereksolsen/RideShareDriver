package com.cs3370.android.lrs_driverapp;

import java.util.Dictionary;
import java.util.Hashtable;

public class Driver {
    private static Driver instance = null;

    private Dictionary mDriverInfo;

    public static Driver getInstance() {
        if (instance == null) {
            instance = new Driver();
        }
        return instance;
    }
    private Driver() {
        mDriverInfo = new Hashtable();
    }

    public String get(String key) {
        return mDriverInfo.get(key).toString();
    }

    public void set(String key, Boolean value) {
        mDriverInfo.put(key, value);
    }

    public void set(String key, String value) {
        mDriverInfo.put(key, value);
    }

}
