package com.atakmap.android.takml_framework;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.atakmap.android.takml_framework.plugin.R;
import com.atakmap.coremap.log.Log;
import com.bbn.tak.ml.sensor.SensorDataStream;
import com.bbn.takml.sensor_framework.SensorTagUpdateListener;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TakMlFrameworkSensorsExpandable extends BaseExpandableListAdapter {

    public static final String TAG = "TakMlFrameworkPluginExpandable";
    public static final String STREAM_NAME_LABEL = "Stream Name:";

    private Context context;
    private List<String> sensorIDs;
    private Map<String, List<String>> sensorIDToSensorDesc;
    private Map<String, String> sensorIDToName;
    private SensorTagUpdateListener sensorTagUpdateListener;

    public TakMlFrameworkSensorsExpandable(Context context, SensorTagUpdateListener listener) {
        this.context = context;
        this.sensorIDs = new LinkedList<String>();
        this.sensorIDToSensorDesc = new HashMap<String, List<String>>();
        this.sensorIDToName = new HashMap<String, String>();
        this.sensorTagUpdateListener = listener;
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return this.sensorIDToSensorDesc.get(this.sensorIDs.get(groupPosition)).get(childPosititon);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        final String childText = (String)getChild(groupPosition, childPosition);

        LayoutInflater infalInflater = (LayoutInflater) this.context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(childText.startsWith(STREAM_NAME_LABEL)) {
            convertView = infalInflater.inflate(R.layout.editable_list_item, null);
            final TextView txtListChild = (TextView) convertView.findViewById(R.id.txtEditableListItem);
            txtListChild.setText(childText.substring(STREAM_NAME_LABEL.length()));
            Button updateSensorTagButton = convertView.findViewById(R.id.updateSensorTagButton);
            updateSensorTagButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String streamTag = txtListChild.getText().toString();
                    String sensorID = TakMlFrameworkSensorsExpandable.this.sensorIDs.get(groupPosition);
                    sensorTagUpdateListener.sensorTagUpdated(sensorID, streamTag);
                    sensorIDToSensorDesc.get(sensorID).remove(3);
                    sensorIDToSensorDesc.get(sensorID).add(STREAM_NAME_LABEL + streamTag);
                }
            });
        } else {
            convertView = infalInflater.inflate(R.layout.list_item, null);
            TextView txtListChild = (TextView) convertView.findViewById(R.id.lblListItem);
            txtListChild.setText(childText);
        }

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.sensorIDToSensorDesc.get(this.sensorIDs.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        String sensorID = this.sensorIDs.get(groupPosition);
        if (sensorID == null)
            return null;

        String sensorName = this.sensorIDToName.get(sensorID);
        if (sensorName == null)
            return null;

        // Return the plugin name and ID as the group label.
        return sensorName + " (ID: " + sensorID + ")";
    }

    @Override
    public int getGroupCount() {
        return this.sensorIDs.size();
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

    public void add(SensorDataStream sensorDataStream) {
        String sensorID = sensorDataStream.getID();
        if (this.sensorIDs.contains(sensorID)) {
            this.sensorIDToName.remove(sensorID);
            this.sensorIDToSensorDesc.remove(sensorID);
        } else {
            this.sensorIDs.add(sensorID);
        }
        this.sensorIDToName.put(sensorID, sensorDataStream.getSensor().getName());
        List<String> items = new LinkedList<String>();
        items.add("ID: " + sensorID);
        items.add("Name: " + sensorDataStream.getSensor().getName());
        items.add("Description: " + sensorDataStream.getSensor().getDescription());
        items.add(STREAM_NAME_LABEL + sensorDataStream.getStreamName());

        this.sensorIDToSensorDesc.put(sensorID, items);
        notifyDataSetChanged();
    }

    public void remove(SensorDataStream sensorDataStream) {
        String sensorID = sensorDataStream.getID();
        if (!this.sensorIDs.contains(sensorID)) {
            Log.w(TAG, "Sensors list does not contain plugin with ID " + sensorID);
            return;
        }
        this.sensorIDToSensorDesc.remove(sensorID);
        this.sensorIDToName.remove(sensorID);
        this.sensorIDs.remove(sensorID);
        notifyDataSetChanged();
    }
}
