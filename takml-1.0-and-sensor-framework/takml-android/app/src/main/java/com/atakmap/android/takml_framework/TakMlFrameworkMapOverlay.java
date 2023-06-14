
package com.atakmap.android.takml_framework;

import android.content.Context;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Toast;

import com.atakmap.android.hierarchy.HierarchyListFilter;
import com.atakmap.android.hierarchy.HierarchyListItem;
import com.atakmap.android.hierarchy.action.GoTo;
import com.atakmap.android.hierarchy.action.Search;
import com.atakmap.android.hierarchy.action.Visibility;
import com.atakmap.android.hierarchy.action.Visibility2;
import com.atakmap.android.hierarchy.items.AbstractHierarchyListItem2;
import com.atakmap.android.hierarchy.items.MapItemUser;
import com.atakmap.android.maps.DeepMapItemQuery;
import com.atakmap.android.maps.DefaultMapGroup;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MapView.RenderStack;
import com.atakmap.android.maps.MetaShape;
import com.atakmap.android.menu.MapMenuReceiver;
import com.atakmap.android.menu.MenuLayoutWidget;
import com.atakmap.android.overlay.AbstractMapOverlay2;
import com.atakmap.android.takml_framework.plugin.R;
import com.atakmap.android.takml_framework.samplelayer.ExampleLayer;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.Layer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Example map overlay that is displayed within Overlay Manager
 */
public class TakMlFrameworkMapOverlay extends AbstractMapOverlay2 {

    private static final String TAG = "TakMlFrameworkStandupMapOverlay";

    private final MapView _mapView;
    private final Context _plugin;
    private final TakMlFrameworkStandupDeepMapItemQuery _query;
    private final DefaultMapGroup _group;

    private TakMlFrameworkStandupListModel _listModel;

    public TakMlFrameworkMapOverlay(MapView mapView, Context plugin) {
        _mapView = mapView;
        _plugin = plugin;
        _query = new TakMlFrameworkStandupDeepMapItemQuery();
        _group = new DefaultMapGroup("TAK-ML Framework Map Group");
        _group.setMetaBoolean("addToObjList", false);
    }

    @Override
    public String getIdentifier() {
        return TAG;
    }

    @Override
    public String getName() {
        return _plugin.getString(R.string.tak_ml_framework);
    }

    @Override
    public MapGroup getRootGroup() {
        return _group;
    }

    @Override
    public DeepMapItemQuery getQueryFunction() {
        return _query;
    }

    @Override
    public HierarchyListItem getListModel(BaseAdapter adapter,
            long capabilities, HierarchyListFilter prefFilter) {
        if (_listModel == null)
            _listModel = new TakMlFrameworkStandupListModel();
        _listModel.refresh(adapter, prefFilter);
        return _listModel;
    }

    private List<ExampleLayer> getLayers() {
        List<ExampleLayer> ret = new ArrayList<>();
        List<Layer> layers = _mapView.getLayers(
                RenderStack.MAP_SURFACE_OVERLAYS);
        for (Layer l : layers) {
            if (l instanceof ExampleLayer) {
                ExampleLayer el = (ExampleLayer) l;
                MetaShape shape = el.getMetaShape();
                if (shape.getGroup() == null)
                    _group.addItem(shape);
                ret.add(el);
            }
        }
        return ret;
    }

    public ExampleLayer findLayer(String uid) {
        for (ExampleLayer l : getLayers()) {
            if (l.getMetaShape().getUID().equals(uid))
                return l;
        }
        return null;
    }

    public class TakMlFrameworkStandupListModel extends AbstractHierarchyListItem2
            implements Search, Visibility2, View.OnClickListener {

        private final static String TAG = "TakMlFrameworkStandupListModel";

        private View _header, _footer;

        public TakMlFrameworkStandupListModel() {
            this.asyncRefresh = true;
        }

        @Override
        public String getTitle() {
            return TakMlFrameworkMapOverlay.this.getName();
        }

        @Override
        public String getIconUri() {
            return "android.resource://" + _plugin.getPackageName()
                    + "/" + R.drawable.tak_ml_icon;
        }

        public int getPreferredListIndex() {
            return 5;
        }

        @Override
        public int getDescendantCount() {
            return 0;
        }

        @Override
        public Object getUserObject() {
            return this;
        }

        @Override
        public View getExtraView() {
            return null;
        }

        @Override
        public View getHeaderView() {
            return null;
        }

        @Override
        public View getFooterView() {
            return null;
        }

        @Override
        public void refreshImpl() {
            List<HierarchyListItem> filtered = new ArrayList<>();
            List<ExampleLayer> layers = getLayers();
            for (ExampleLayer l : layers) {
                LayerHierarchyListItem item = new LayerHierarchyListItem(l);
                if (this.filter.accept(item))
                    filtered.add(item);
            }

            // Sort
            sortItems(filtered);

            // Update
            updateChildren(filtered);
        }

        @Override
        public void dispose() {
            disposeChildren();
        }

        @Override
        public boolean hideIfEmpty() {
            return true;
        }

        @Override
        public boolean isMultiSelectSupported() {
            return false;
        }

        @Override
        public boolean setVisible(boolean visible) {
            List<Visibility> actions = getChildActions(Visibility.class);
            boolean ret = !actions.isEmpty();
            for (Visibility del : actions)
                ret &= del.setVisible(visible);
            return ret;
        }

        @Override
        public Set<HierarchyListItem> find(String searchTerms) {
            searchTerms = searchTerms.toLowerCase();
            Set<HierarchyListItem> results = new HashSet<HierarchyListItem>();
            List<HierarchyListItem> items = getChildren();
            for (HierarchyListItem item : items) {
                if (item.getTitle().toLowerCase().contains(searchTerms))
                    results.add(item);
            }
            return results;
        }

        @Override
        public void onClick(View v) {
            if (v instanceof Button)
                Toast.makeText(_mapView.getContext(),
                        ((Button) v).getText(),
                        Toast.LENGTH_LONG).show();
        }
    }

    private class LayerHierarchyListItem extends AbstractHierarchyListItem2
            implements Visibility, GoTo, MapItemUser {

        private final ExampleLayer _layer;

        LayerHierarchyListItem(ExampleLayer layer) {
            _layer = layer;
        }

        @Override
        public String getTitle() {
            return _layer.getName();
        }

        @Override
        public String getDescription() {
            return _plugin.getString(R.string.example_layer_description);
        }

        @Override
        public String getIconUri() {
            return "android.resource://" + _mapView.getContext()
                    .getPackageName() + "/"
                    + com.atakmap.app.R.drawable.ic_overlay_gridlines;
        }

        @Override
        public Object getUserObject() {
            return _layer;
        }

        @Override
        public boolean isChildSupported() {
            return false;
        }

        @Override
        public int getDescendantCount() {
            return 0;
        }

        @Override
        public void refreshImpl() {
        }

        @Override
        public boolean hideIfEmpty() {
            return false;
        }

        @Override
        public boolean setVisible(boolean visible) {
            if (visible != _layer.isVisible()) {
                _layer.setVisible(visible);
                return true;
            }
            return false;
        }

        @Override
        public boolean isVisible() {
            return _layer.isVisible();
        }

        @Override
        public MapItem getMapItem() {
            return _layer.getMetaShape();
        }

        @Override
        public boolean goTo(boolean select) {
            ATAKUtilities.scaleToFit(_mapView, _layer
                    .getPoints(),
                    _mapView.getWidth(), _mapView.getHeight());
            if (select) {
                MenuLayoutWidget mw = MapMenuReceiver.getMenuWidget();
                if (mw != null) {
                    mw.openMenuOnItem(_layer.getMetaShape());
                    return true;
                }
            }
            return false;
        }
    }

    private class TakMlFrameworkStandupDeepMapItemQuery implements DeepMapItemQuery {

        @Override
        public MapItem deepFindItem(Map<String, String> metadata) {
            return null;
        }

        @Override
        public List<MapItem> deepFindItems(Map<String, String> metadata) {
            return null;
        }

        @Override
        public MapItem deepFindClosestItem(GeoPoint location, double threshold,
                Map<String, String> metadata) {
            return null;
        }

        @Override
        public Collection<MapItem> deepFindItems(GeoPoint location,
                double radius, Map<String, String> metadata) {
            return null;
        }

        @Override
        public MapItem deepHitTest(int xpos, int ypos, GeoPoint point,
                MapView view) {
            for (ExampleLayer l : getLayers()) {
                if (l.isVisible() && l.getBounds().contains(point))
                    return l.getMetaShape();
            }
            return null;
        }

        @Override
        public SortedSet<MapItem> deepHitTestItems(int xpos, int ypos,
                GeoPoint point, MapView view) {
            SortedSet<MapItem> ret = new TreeSet<>(
                    MapItem.ZORDER_HITTEST_COMPARATOR);
            for (ExampleLayer l : getLayers()) {
                if (l.isVisible() && l.getBounds().contains(point))
                    ret.add(l.getMetaShape());
            }
            return ret;
        }

        @Override
        public Collection<MapItem> deepFindItems(GeoBounds bounds,
                Map<String, String> metadata) {
            SortedSet<MapItem> ret = new TreeSet<>(
                    MapItem.ZORDER_HITTEST_COMPARATOR);
            for (ExampleLayer l : getLayers()) {
                if (l.isVisible() && l.getBounds().intersects(bounds))
                    ret.add(l.getMetaShape());
            }
            return ret;
        }
    }
}
