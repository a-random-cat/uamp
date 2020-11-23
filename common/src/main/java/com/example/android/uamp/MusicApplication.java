package com.example.android.uamp;

import android.app.Application;

public class MusicApplication extends Application {
    private static MusicApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static MusicApplication getInstance() {
        return instance;
    }
}
