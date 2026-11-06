package com.example.assign1;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Applies the saved theme (light / dark) as soon as the app process starts,
 * so every screen uses the right theme.
 */
public class App extends Application {

    public static final String PREFS_NAME = "settings";
    public static final String KEY_NIGHT_MODE = "night_mode";

    @Override
    public void onCreate() {
        super.onCreate();
        int mode = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}