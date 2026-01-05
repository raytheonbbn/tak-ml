package com.atakmap.android.takml_android.emm;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_android.hooks.MobileManagementManager;

public class TestMobileManagementManager extends MobileManagementManager {

    @Override
    public void start(Context context) {
        boolean isUsingPluginContext = context.getClass().getName().equals(PLUGIN_CONTEXT_CLASS);
        if (isUsingPluginContext) {
            deviceId = MapView.getDeviceUid();
        }

        Intent intent = new Intent(context, FakeEmmApiService.class);
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
}
