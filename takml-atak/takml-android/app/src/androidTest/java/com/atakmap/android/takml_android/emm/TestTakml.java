package com.atakmap.android.takml_android.emm;

import android.content.Context;

import com.atakmap.android.takml_android.Takml;
import com.atakmap.android.takml_android.hooks.MobileManagementManager;
import com.atakmap.android.takml_android.net.TakmlServerClient;

public class TestTakml extends Takml {
    public static boolean enableEmm = false;

    /**
     * Initializes a Takml object. Accepts ATAK plugin context or generic Activity context.
     * Note, the latter does not currently support {@link Takml#showConfigUI(Intent)}.
     *
     * @param pluginOrActivityContext - ATAK plugin or Android Activity context
     * @throws TakmlInitializationException
     */
    public TestTakml(Context pluginOrActivityContext) {
        super(pluginOrActivityContext);
    }

    @Override
    protected void initializeEmm(Context context) {
        mobileManagementManager = new TestMobileManagementManager();

        if(enableEmm) {
            // Set up Enterprise Mobile Management Manager Hooks
            mobileManagementManager.start(context);
        }
    }

    public TakmlServerClient createTakmlServerClient(String url) {
        return new TakmlServerClient(url, true);
    }
}
