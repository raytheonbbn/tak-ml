
package com.atakmap.android.takml_framework.samplelayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.atakmap.android.maps.MetaShape;
import com.atakmap.android.menu.PluginMenuParser;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.coords.MutableGeoBounds;
import com.atakmap.map.layer.AbstractLayer;

import java.util.UUID;

public class ExampleLayer extends AbstractLayer {

    public static final String TAG = "ExampleLayer";

    int[] frameRGB;
    int frameWidth;
    int frameHeight;

    GeoPoint upperLeft;
    GeoPoint upperRight;
    GeoPoint lowerRight;
    GeoPoint lowerLeft;

    private final Context pluginContext;
    private final String uri;
    private final MetaShape metaShape;

    public ExampleLayer(Context plugin, final String name, final String uri) {
        super(name);

        this.pluginContext = plugin;
        this.uri = uri;

        this.upperLeft = GeoPoint.createMutable();
        this.upperRight = GeoPoint.createMutable();
        this.lowerRight = GeoPoint.createMutable();
        this.lowerLeft = GeoPoint.createMutable();

        final Bitmap bitmap = BitmapFactory.decodeFile(uri);
        upperLeft.set(50, -50);
        upperRight.set(50, -40);
        lowerRight.set(40, -40);
        lowerLeft.set(40, -50);

        frameWidth = bitmap.getWidth();
        frameHeight = bitmap.getHeight();
        Log.d(TAG,
                "decode file: " + uri + " " + frameWidth + " " + frameHeight);
        frameRGB = new int[frameHeight * frameWidth];

        bitmap.getPixels(frameRGB, 0, frameWidth, 0, 0, frameWidth,
                frameHeight);

        metaShape = new MetaShape(UUID.randomUUID().toString()) {
            @Override
            public GeoPointMetaData[] getMetaDataPoints() {
                return GeoPointMetaData.wrap(ExampleLayer.this.getPoints());
            }
            @Override
            public GeoPoint[] getPoints() {
                return ExampleLayer.this.getPoints();
            }

            @Override
            public GeoBounds getBounds(MutableGeoBounds bounds) {
                return ExampleLayer.this.getBounds();
            }
        };
        metaShape.setMetaString("callsign", TAG);
        metaShape.setMetaString("shapeName", TAG);
        metaShape.setType("takml_framework_layer");
        metaShape.setMetaString("menu", PluginMenuParser.getMenu(
                pluginContext, "menus/layer_menu.xml"));
    }

    public GeoBounds getBounds() {
        return GeoBounds.createFromPoints(getPoints());
    }

    public GeoPoint[] getPoints() {
        return new GeoPoint[] {
                upperLeft, upperRight, lowerRight, lowerLeft
        };
    }

    public MetaShape getMetaShape() {
        return metaShape;
    }
}
