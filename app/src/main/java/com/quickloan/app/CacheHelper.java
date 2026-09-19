package com.quickloan.app;

import android.content.Context;
import android.content.SharedPreferences;

public class CacheHelper {

    private static final String PREF_NAME = "quickloan_cache";

    public static void saveCache(Context context, String key, String jsonData) {
        if (context == null || jsonData == null) return;
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().putString(key, jsonData).apply();
    }

    public static String getCache(Context context, String key) {
        if (context == null) return null;
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return pref.getString(key, null);
    }

    public static void clearCache(Context context) {
        if (context == null) return;
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pref.edit().clear().apply();
    }
}
