package com.atakmap.android.atakutils.layers;

import com.atakmap.android.importfiles.task.ImportFileTask;
import com.atakmap.android.maps.MapView;

import java.io.File;
import java.util.List;

/**
 * Common interface for interacting with map overlays like vector and raster images.
 * @author Benjamin Toll
 */
public abstract class LayerManager {
    protected MapView mapView;

    public LayerManager() {
        mapView = MapView.getMapView();
    }

    public void refreshLayer(File layerFile){
        ImportFileTask importTask = new ImportFileTask(mapView.getContext(), null);
        importTask.addFlag(ImportFileTask.FlagValidateExt
                | ImportFileTask.FlagShowNotificationsDuringImport);
        importTask.execute(new String[]{layerFile.getAbsolutePath()});
    }

    /**
     * Get the list of layer names.
     * @return the list of layer names.
     */
    public abstract List<String> getLayerNames();

    /**
     *
     * @param name layer name.
     * @return true if layer is visible, false otherwise.
     */
    public abstract boolean isVisible(String name);

    /**
     * Toggle the visibility of a feature.
     * @param name the name of the feature.
     * @param visible true if the feature should be made visible, false otherwise.
     */
    public abstract boolean toggleFeature(String name, boolean visible);

    /**
     * Toggle the visibility of a feature set.
     * @param name the name of the feature set.
     * @param visible true if the feature set should be made visible, false otherwise.
     */
    public abstract boolean toggleFeatureSet(String name, boolean visible);

    /**
     * Delete the feature set.
     * @param name the name of the feature set to delete.
     * @return true if the feature set is successfully deleted.
     */
    public abstract boolean deleteFeatureSet(String name);
}
