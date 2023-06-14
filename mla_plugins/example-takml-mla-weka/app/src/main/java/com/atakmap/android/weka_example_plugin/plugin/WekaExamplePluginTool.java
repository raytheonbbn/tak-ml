
package com.atakmap.android.weka_example_plugin.plugin;

import android.content.Context;

import com.atak.plugins.impl.AbstractPluginTool;
import com.atakmap.android.weka_example_plugin.WekaExamplePluginDropDownReceiver;

import gov.tak.api.util.Disposable;

/**
 * Please note:
 *     Support for versions prior to 4.5.1 can make use of a copy of AbstractPluginTool shipped with
 *     the plugin.
 */
public class WekaExamplePluginTool extends AbstractPluginTool
        implements Disposable {

    public WekaExamplePluginTool(Context context) {
        super(context,
                context.getString(R.string.app_name),
                context.getString(R.string.app_name),
                context.getResources().getDrawable(R.drawable.ic_launcher),
                WekaExamplePluginDropDownReceiver.SHOW_PLUGIN);
    }

    @Override
    public void dispose() {
    }
}
