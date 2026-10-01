package com.eggyhub.android;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class PluginProxyActivity extends Activity {
    private static final String TAG = "PluginProxyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "PluginProxyActivity created");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "PluginProxyActivity destroyed");
    }
}