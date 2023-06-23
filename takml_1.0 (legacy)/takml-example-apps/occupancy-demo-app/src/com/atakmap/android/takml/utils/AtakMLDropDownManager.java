package com.atakmap.android.takml.utils;

import android.util.Log;

import com.atakmap.android.takml.receivers.OccupancyDetectorDropDownReceiver;

import java.util.List;
import java.util.Stack;

public class AtakMLDropDownManager {
    private static final String TAG = AtakMLDropDownManager.class.getSimpleName();
    private static AtakMLDropDownManager dropDownManager;
    private List<String> backstack;

    public static synchronized AtakMLDropDownManager getInstance() {
        if (dropDownManager == null) {
            dropDownManager = new AtakMLDropDownManager();
        }
        return dropDownManager;
    }

    public AtakMLDropDownManager() {
        backstack = new Stack<>();
    }

    /**
     * Pushes an intent action to display a receiver onto the back stack
     * @param action the action to display this receiver
     */
    public void addToBackStack(String action) {
        backstack.add(action);
    }

    /**
     * Removes an intent action to display a receiver from the back stack
     */
    public String removeFromBackStack() {
        if (!backstack.isEmpty()) {
            return backstack.remove(backstack.size()-1);
        } else {
            Log.w(TAG, "Back stack is empty");
            return OccupancyDetectorDropDownReceiver.SHOW_PLUGIN;
        }
    }

    /**
     * Removes all stored entries from the back stack
     */
    public void clearBackStack() {
        backstack.clear();
    }

    public boolean isEmpty() {
        return backstack.isEmpty();
    }

    public int size() {
        return backstack.size();
    }

    public String top() {
        if (!backstack.isEmpty()) {
            return backstack.get(backstack.size()-1);
        } else {
            Log.w(TAG, "Back stack is empty");
            return OccupancyDetectorDropDownReceiver.SHOW_PLUGIN;
        }
    }
}
