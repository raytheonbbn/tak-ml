package com.atakmap.android.atakutils.layers;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.atakmap.android.grg.GRGMapComponent;
import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.importexport.ImportReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.MultiLayer;
import com.atakmap.map.layer.raster.DatasetRasterLayer2;
import com.atakmap.map.layer.raster.LocalRasterDataStore;
import com.atakmap.map.layer.raster.RasterDataStore;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides an API for interacting with GRG/Raster layers.
 * @author Benjamin Toll
 */
public class GrgLayerManager extends LayerManager {
    private static final String TAG = GrgLayerManager.class.getSimpleName();

    private static final GrgLayerManager MANAGER = new GrgLayerManager();

    private GrgLayerManager() {
        super();
    }

    public static GrgLayerManager getInstance() {
        return MANAGER;
    }

    @Override
    public List<String> getLayerNames() {
        /* TODO Turning a Collection into a List and then using its order is bad.
        We should work hard to convince ourselves that names is
        really the right structure to be looking at to determine layer order.
        */
        List<String> layerNames = new ArrayList<>();
        DatasetRasterLayer2 grgLayer = findGrgLayer();
        if (grgLayer!=null){
            RasterDataStore dataStore = grgLayer.getDataStore();
            if (dataStore != null) {
                Collection<String> datasetNames = dataStore.getDatasetNames();
                if (datasetNames != null && !datasetNames.isEmpty()) {
                    layerNames.addAll(datasetNames);
                }
            }
        }
        return layerNames;
    }

    @Override
    public boolean isVisible(String name) {
        DatasetRasterLayer2 grgLayer = findGrgLayer();
        return grgLayer.isVisible(name);
    }

    @Override
    public boolean toggleFeature(String name, boolean visible) {
        Log.w(TAG, "Toggling an individual GRG layer is not yet implemented");
        return false;
    }

    @Override
    public boolean toggleFeatureSet(String name, boolean visible) {
        DatasetRasterLayer2 grgLayer = findGrgLayer();
        grgLayer.setVisible(name, visible);
        /* this is really stupid.  The method to fire all the listeners is protected, but it
         * gets called any time you change the visibility of the entire grg layer.  Since we
         * never make it invisible, we can just make it visible over and over again whenever
         * we want a refresh.  Blegh.*/
        grgLayer.setVisible(true);
        return true;
    }

    @Override
    public boolean deleteFeatureSet(String name) {
        RasterDataStore.DatasetQueryParameters params = new RasterDataStore.DatasetQueryParameters(); //
        params.names = Collections.singleton(name); //
        Collection<String> pathsToDelete = getGrgFiles(name);
        for (String path : pathsToDelete) {
            Log.d(TAG, "Deleting " + name + " at " + path);
            Intent i = new Intent(
                    ImportExportMapComponent.ACTION_DELETE_DATA);
            i.putExtra(ImportReceiver.EXTRA_CONTENT,
                    GRGMapComponent.IMPORTER_CONTENT_TYPE);
            i.putExtra(ImportReceiver.EXTRA_MIME_TYPE,
                    GRGMapComponent.IMPORTER_DEFAULT_MIME_TYPE);
            i.putExtra(ImportReceiver.EXTRA_URI, path);
            AtakBroadcast.getInstance().sendBroadcast(i);
        }
        return true;
    }

    private DatasetRasterLayer2 findGrgLayer() {
        DatasetRasterLayer2 foundLayer=null;
        List<Layer> layers = mapView.getLayers(MapView.RenderStack.RASTER_OVERLAYS);
        for (Layer layer : layers) {
            if (layer instanceof MultiLayer && layer.getName().equals("GRG")) {
                List<Layer> subLayers = ((MultiLayer) layer).getLayers();
                for (Layer subLayer : subLayers) {
                    if (subLayer.getName().equals("GRG rasters") && subLayer instanceof DatasetRasterLayer2) {
                        //Log.d(TAG, "Found a raster layer");
                        foundLayer = (DatasetRasterLayer2) subLayer;
                    }
                }
            }
        }
        return foundLayer;
    }

    /**
     * Adapted from GRGMapOverlay
     * @see com.atakmap.android.grg.GRGMapOverlay
     * @param name the name of the GRG to delete
     * @return a collection of fully qualified GRG names to be deleted (only expecting there to be one)
     */
    private Set<String> getGrgFiles(String name) {
        RasterDataStore.DatasetQueryParameters params = new RasterDataStore.DatasetQueryParameters();
        params.names = Collections.singleton(name);
        RasterDataStore grgLayersDb = findGrgLayer().getDataStore();

        Log.d(TAG, "files for " + name);
        Set<String> retval = new HashSet<String>();
        RasterDataStore.DatasetDescriptorCursor result = null;
        try {
            result = grgLayersDb.queryDatasets(params);
            while (result.moveToNext()) {
                File file = null;
                if (grgLayersDb instanceof LocalRasterDataStore)
                    file = ((LocalRasterDataStore) grgLayersDb)
                            .getFile(result.get());

                if (file == null) {
                    file = new File(result.get().getUri());
                    String f = result.get().getUri();
                    Log.d(TAG, "uri: " + f);
                    if (FileSystemUtils.isFile(f)) {
                        file = new File(FileSystemUtils
                                .sanitizeWithSpacesAndSlashes(f));
                    } else {
                        try {
                            Uri uri = Uri.parse(f);
                            if (uri != null && uri.getPath() != null) {
                                if (FileSystemUtils.isFile(uri.getPath()))
                                    file = new File(FileSystemUtils
                                            .sanitizeWithSpacesAndSlashes(
                                                    uri.getPath()));
                                else
                                    file = null;
                            }
                        } catch (Exception e) {
                            Log.w(TAG,
                                    "Exception occurred while obtaining file for "
                                            + name,
                                    e);
                        }
                    }

                }

                if (file != null)
                    retval.add(file.getAbsolutePath());
            }
        } finally {
            if (result != null)
                result.close();
        }

        return retval;
    }
}
