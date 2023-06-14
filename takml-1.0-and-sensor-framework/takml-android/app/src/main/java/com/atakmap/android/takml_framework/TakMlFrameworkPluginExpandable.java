package com.atakmap.android.takml_framework;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.atakmap.android.takml_framework.plugin.R;
import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.mx_framework.MXPluginDescription;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TakMlFrameworkPluginExpandable extends BaseExpandableListAdapter {

    public static final String TAG = "TakMlFrameworkPluginExpandable";

    private Context context;
    private List<String> pluginIDs;
    private Map<String, List<String>> pluginIDToPluginDesc;
    private Map<String, String> pluginIDToName;

    public TakMlFrameworkPluginExpandable(Context context) {
        this.context = context;
        this.pluginIDs = new LinkedList<String>();
        this.pluginIDToPluginDesc = new HashMap<String, List<String>>();
        this.pluginIDToName = new HashMap<String, String>();
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return this.pluginIDToPluginDesc.get(this.pluginIDs.get(groupPosition))
                .get(childPosititon);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        final String childText = (String)getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_item, null);
        }

        TextView txtListChild = (TextView) convertView
                .findViewById(R.id.lblListItem);

        txtListChild.setText(childText);
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.pluginIDToPluginDesc.get(this.pluginIDs.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        String pluginID = this.pluginIDs.get(groupPosition);
        if (pluginID == null)
            return null;

        String pluginName = this.pluginIDToName.get(pluginID);
        if (pluginName == null)
            return null;

        // Return the plugin name and ID as the group label.
        return pluginName + " (ID: " + pluginID + ")";
    }

    @Override
    public int getGroupCount() {
        return this.pluginIDs.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_group, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public void add(MXPluginDescription desc) {
        if (this.pluginIDs.contains(desc.id())) {
            this.pluginIDToName.remove(desc.id());
            this.pluginIDToPluginDesc.remove(desc.id());
        } else {
            this.pluginIDs.add(desc.id());
        }
        this.pluginIDToName.put(desc.id(), desc.name());
        List<String> items = new LinkedList<String>();
        items.add("ID: " + desc.id());
        items.add("Name: " + desc.name());
        items.add("Author: " + desc.author());
        items.add("Library: " + desc.library());
        items.add("Algorithm: " + desc.algorithm());
        items.add("Library version: " + desc.version());
        items.add("Client-side: " + desc.clientSide());
        items.add("Server-side: " + desc.serverSide());
        items.add("Description: " + desc.description());
        this.pluginIDToPluginDesc.put(desc.id(), items);
        notifyDataSetChanged();
    }

    public void remove(MXPluginDescription desc) {
        if (!this.pluginIDs.contains(desc.id())) {
            Log.w(TAG, "UI plugins list does not contain plugin with ID " + desc.id());
            return;
        }
        this.pluginIDToPluginDesc.remove(desc.id());
        this.pluginIDToName.remove(desc.id());
        this.pluginIDs.remove(desc.id());
        notifyDataSetChanged();
    }
}
