package com.atakmap.android.takml_android.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.takml_android.R;

import java.util.ArrayList;
import java.util.List;

public class ModelImportCustomAdapter extends BaseAdapter {
    private final List<String> options;
    private final Context pluginContext;

    public ModelImportCustomAdapter(Context pluginContext, List<String> options) {
        this.pluginContext = pluginContext;
        this.options = options;
    }

    @Override
    public int getCount() {
        return options.size();
    }

    @Override
    public Object getItem(int i) {
        return options.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
                (LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout ll = new LinearLayout(pluginContext);
        ll.setLayoutParams(params);

        float density = MapView.getMapView().getContext().getResources().getDisplayMetrics().density;

        TextView category = new TextView(pluginContext);
        category.setBackground(pluginContext.getDrawable(R.drawable.btn_gray));
        category.setShadowLayer(2, 0, 1, R.color.darker_gray);
        category.setText(options.get(position));
        category.setTextColor(Color.WHITE);
        category.setTextAppearance(R.style.darkButton);
        category.setPadding(6, 6, 6, 6);
        category.setHeight((int) (50 * density));
        category.setGravity(Gravity.CENTER);

        params = new LinearLayout.LayoutParams
                (LinearLayout.LayoutParams
                        .MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        category.setLayoutParams(params);
        ll.addView(category);

        return ll;
    }
}
