package com.plump_monkey.iotble;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class Settings {

    private static Settings instance;
    private short scrolling_delay=500;
    private boolean filter_unpaired_devices=false;

    private static final String SETTINGS_FILE = "com.plump_monkey.iotble.settings_file";
    private static final String SCROLLING_DELAY = "scrolling_delay";
    private static final String FILTER_UNPAIRED_DEVICES = "filter_unpaired_devices";

    private Settings() {
    }

    public static synchronized Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    public void save(Context context) {
        Log.d(Constants.TAG,"Saving preferences");
        SharedPreferences sharedPref = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(SCROLLING_DELAY, scrolling_delay);
        editor.putBoolean(FILTER_UNPAIRED_DEVICES, filter_unpaired_devices);
        editor.commit();
    }


    public void restore(Context context) {
        Log.d(Constants.TAG,"Restoring preferences");
        SharedPreferences sharedPref = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE);
        scrolling_delay = (short)  sharedPref.getInt(SCROLLING_DELAY,500);
        filter_unpaired_devices = sharedPref.getBoolean(FILTER_UNPAIRED_DEVICES,false);
    }

    public short getScrolling_delay() {
        return scrolling_delay;
    }

    public void setScrolling_delay(short scrolling_delay) {
        this.scrolling_delay = scrolling_delay;
    }

    public boolean isFilter_unpaired_devices() {
        return filter_unpaired_devices;
    }

    public void setFilter_unpaired_devices(boolean filter_unpaired_devices) {
        this.filter_unpaired_devices = filter_unpaired_devices;
    }

}

