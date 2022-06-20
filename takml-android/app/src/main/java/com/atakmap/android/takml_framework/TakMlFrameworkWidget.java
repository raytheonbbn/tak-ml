
package com.atakmap.android.takml_framework;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.view.MotionEvent;

import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.widgets.AbstractWidgetMapComponent;
import com.atakmap.android.widgets.MapWidget;
import com.atakmap.android.widgets.MapWidget.OnPressListener;
import com.atakmap.android.widgets.MarkerIconWidget;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.assets.Icon;

public class TakMlFrameworkWidget extends AbstractWidgetMapComponent implements
        OnPressListener {

    private MapView mapView;

    private final static int ICON_WIDTH = 96;
    private final static int ICON_HEIGHT = 64;
    public static final String TAG = "TakMlFrameworkStandupWidget";

    @Override
    protected void onCreateWidgets(final Context context, final Intent intent,
            final MapView view) {

        mapView = view;
        Log.d(TAG, "registering my wacky search");
        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction("SHOW_MY_WACKY_SEARCH");
        this.registerReceiver(view.getContext(), wacky, filter);
        Log.d(TAG, "registered my wacky search");

    }

    final BroadcastReceiver wacky = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "wacky search");

            MarkerIconWidget widget = new MarkerIconWidget();

            String imageUri = "android.resource://"
                    + mapView.getContext().getPackageName() + "/"
                    + com.atakmap.app.R.drawable.sync_search;
            float density = (float)mapView.getDisplayDpi() / 240f;
            widget.setPoint((mapView.getWidth() / 2)
                    - (density * ICON_WIDTH / 2) + 72, 80f);

            Icon.Builder builder = new Icon.Builder();
            builder.setAnchor(0, 0);
            builder.setColor(Icon.STATE_DEFAULT, Color.WHITE);
            builder.setSize(ICON_WIDTH, ICON_HEIGHT);
            builder.setImageUri(Icon.STATE_DEFAULT, imageUri);

            Icon icon = builder.build();
            widget.setIcon(icon);

            widget.addOnPressListener(TakMlFrameworkWidget.this);
            if (!getRootLayoutWidget().getChildWidgets().contains(widget))
                getRootLayoutWidget().addWidget(widget);
        }
    };

    @Override
    public void onMapWidgetPress(MapWidget widget, MotionEvent event) {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                mapView.getContext());
        builderSingle.setTitle("Wacky Search");
        builderSingle.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                            int which) {
                        dialog.dismiss();
                    }
                });
        builderSingle.show();

    }

    @Override
    protected void onDestroyWidgets(Context context, MapView view) {
    }

    @Override
    public void onStart(Context context, MapView view) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStop(Context context, MapView view) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onPause(Context context, MapView view) {
    }

    @Override
    public void onResume(Context context, MapView view) {
        // TODO Auto-generated method stub

    }
}
