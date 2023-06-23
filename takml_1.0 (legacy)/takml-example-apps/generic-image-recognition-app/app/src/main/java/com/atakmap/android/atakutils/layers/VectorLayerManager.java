package com.atakmap.android.atakutils.layers;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.importexport.ImportReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureCursor;
import com.atakmap.map.layer.feature.FeatureDataStore;
import com.atakmap.map.layer.feature.FeatureLayer;
import com.google.common.collect.Sets;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Provides an API for working with vector layers.
 * @author Benjamin Toll
 */
public class VectorLayerManager extends LayerManager {
    public static final String TAG = VectorLayerManager.class.getSimpleName();

    /**
     * All the layers which we store as vectors.  Note that some of these may not exist in some sessions.
     * This is a list of the names of layers which, should they exist, would be vector based.
     */

    private static final VectorLayerManager MANAGER = new VectorLayerManager();


    public static VectorLayerManager getInstance() {
        return MANAGER;
    }

    @Override
    public List<String> getLayerNames() {
        FeatureDataStore dataStore = getFeatureDatastore();
        if (dataStore == null) {
            Log.e(TAG, "Could not find vector feature data store");
            return new ArrayList<>();
        }
        ArrayList<String> layerNames = new ArrayList<>();

        //now find the vector layer names that actually exist.  (which may be a subset of VECTOR_LAYER_PREFIXES)
        FeatureDataStore.FeatureSetCursor featureSetCursor = dataStore.queryFeatureSets(null);
        try {
            while (featureSetCursor.moveToNext()) {
                layerNames.add(featureSetCursor.get().getName());
            }
            return layerNames;
        } finally {
            featureSetCursor.close();
        }
    }

    @Override
    public boolean isVisible(String name) {
        FeatureDataStore dataStore = getFeatureDatastore();
        if (dataStore == null) {
            Log.e(TAG, "Could not find vector feature data store");
            return false;
        }
        FeatureDataStore.FeatureSetQueryParameters query = new FeatureDataStore.FeatureSetQueryParameters();
        query.names = Collections.singleton(name);
        query.visibleOnly = true;
        FeatureDataStore.FeatureSetCursor featureSetCursor = dataStore.queryFeatureSets(query);
        try {
            return featureSetCursor.moveToNext();
        } finally {
            featureSetCursor.close();
        }
    }

    @Override
    public boolean toggleFeature(String name, boolean visible) {
        FeatureDataStore dataStore = getFeatureDatastore();
        if (dataStore == null) {
            Log.e(TAG, "Could not find vector feature data store");
            return false;
        }
        FeatureDataStore.FeatureQueryParameters params = new FeatureDataStore.FeatureQueryParameters();
        params.featureNames = Collections.singletonList(name);
        try {
            dataStore.setFeaturesVisible(params, visible);
        } catch (Exception e) {
            Log.e(TAG, "Unable to toggle feature", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean toggleFeatureSet(String name, boolean visible) {
        FeatureDataStore dataStore = getFeatureDatastore();
        if (dataStore == null) {
            Log.e(TAG, "Could not find vector feature data store");
            return false;
        }
        FeatureDataStore.FeatureSetQueryParameters query = new FeatureDataStore.FeatureSetQueryParameters();
        query.names = Collections.singleton(name);
        try {
            dataStore.setFeatureSetsVisible(query, visible);
        } catch (Exception e) {
            Log.e(TAG, "Unable to toggle feature set", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteFeatureSet(String name) {
        File file = new File(name);
        Log.d(TAG, "Delete: " + file.getAbsolutePath());
        Intent deleteIntent = new Intent();
        deleteIntent.setAction(ImportExportMapComponent.ACTION_DELETE_DATA);
        deleteIntent.putExtra(ImportReceiver.EXTRA_CONTENT, "kml");
        deleteIntent.putExtra(ImportReceiver.EXTRA_MIME_TYPE,
                "application/vnd.google-earth.kml+xml");
        deleteIntent.putExtra(ImportReceiver.EXTRA_URI, Uri.fromFile(file).toString());
        AtakBroadcast.getInstance().sendBroadcast(deleteIntent);
        return true;
    }

    public List<Feature> getFeatures(String featsetName) {
        List<Feature> result = new ArrayList<>();
        FeatureDataStore dataStore = getFeatureDatastore();
        if (dataStore == null) {
            Log.e(TAG, "Could not find vector feature data store");
            return new ArrayList<>();
        }
        FeatureDataStore.FeatureQueryParameters params = new FeatureDataStore.FeatureQueryParameters();
        params.featureSets = Collections.singletonList(featsetName);
        FeatureCursor cursor = dataStore.queryFeatures(params);
        try {
            while (cursor.moveToNext()) {
                Feature feature = cursor.get();
                result.add(feature);
            }
            return result;
        } finally {
            cursor.close();
        }
    }

    private FeatureDataStore getFeatureDatastore() {
        List<Layer> layers = mapView.getLayers(MapView.RenderStack.VECTOR_OVERLAYS);
        if (layers.isEmpty()) {
            Log.e(TAG, "No vector metalayer found", new IllegalStateException());
            return null;
        }
        return ((FeatureLayer) layers.get(0)).getDataStore();
    }
}
