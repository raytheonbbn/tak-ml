package com.atakmap.android.takml_android.util;

import android.content.Context;
import android.util.Log;

import com.atakmap.android.takml_android.MXPlugin;
import com.atakmap.android.takml_android.lib.TakmlInitializationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class MxPluginsUtil {
    private static final String TAG = MxPluginsUtil.class.getName();

    public static final String MX_PLUGIN_TOKEN_PREFIX = "mx_plugin_";
    public static final String MX_PLUGIN_TOKEN_SUFFIX = ".txt";


    public static ConcurrentMap<String, Set<String>> discoverMxPlugins(Context context){
        ConcurrentMap<String, Set<String>> ret = new ConcurrentHashMap<>();

        String[] assetFiles;
        try {
            assetFiles = context.getAssets().list("");
        } catch (IOException e) {
            Log.e(TAG, "Could not load assets", e);
            return null;
        }

        for(String assetFile : assetFiles){
            if(!assetFile.startsWith(MX_PLUGIN_TOKEN_PREFIX)){
                continue;
            }
            Log.d(TAG, "instantiateMxPlugins: " + assetFile);
            String uuidStr = assetFile.replace(MX_PLUGIN_TOKEN_PREFIX, "");
            uuidStr = uuidStr.replace(MX_PLUGIN_TOKEN_SUFFIX, "");
            uuidStr = uuidStr.replaceAll("(.{8})(.{4})(.{4})(.{4})(.+)", "$1-$2-$3-$4-$5");
            try{
                UUID uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e){
                Log.w(TAG, "asset does not have a valid uuid, skipping", e);
                continue;
            }
            Log.d(TAG, "Found raw file: " + assetFile);

            InputStream bytes;
            try {
                bytes = context.getAssets().open(assetFile);
            } catch (IOException e) {
                Log.w(TAG, "instantiateMxPlugins: " + assetFile, e);
                continue;
            }
            String mxPluginClassName = new BufferedReader(
                    new InputStreamReader(bytes, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            Log.d(TAG, "Found mx plugin: " + mxPluginClassName);
            MXPlugin mxPlugin;
            try {
                mxPlugin = constructMxPlugin(mxPluginClassName);
            }catch (Exception e){
                Log.e(TAG, "Could not instantiate mx plugin: " + mxPluginClassName, e);
                continue;
            }
            if(mxPlugin.getApplicableModelExtensions() == null){
                Log.e(TAG, "Could not instantiate mx plugin: " + mxPluginClassName
                        + ", null applicable model extensions");
                continue;
            }
            for(String extension : mxPlugin.getApplicableModelExtensions()) {
                ret.computeIfAbsent(extension, k ->
                        new HashSet<>()).add(mxPluginClassName);
            }
        }
        return ret;
    }

    public static MXPlugin constructMxPlugin(String className) throws Exception{
        Class<? extends MXPlugin> mxPluginClass;
        try {
            mxPluginClass = Class.forName(className).asSubclass(MXPlugin.class);
        } catch (ClassNotFoundException e) {
            throw new TakmlInitializationException("class not found exception instantiateMxPluginViaReflection", e);
        } catch (ClassCastException e) {
            throw new TakmlInitializationException("class cast exception instantiateMxPluginViaReflection", e);
        }
        Log.d(TAG, "Constructing MXPlugin with class: " + mxPluginClass.getName());
        Constructor<? extends MXPlugin> constructor;
        try {
            constructor = mxPluginClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new TakmlInitializationException("Could not find constructor in class with name " + className,
                    e);
        } catch (SecurityException e) {
            throw new TakmlInitializationException("Security Exception with name " + className, e);
        }
        MXPlugin mxPlugin;
        try {
            mxPlugin = constructor.newInstance();
        } catch (InstantiationException e) {
            throw new TakmlInitializationException(
                    "Instantiation error, could not create instance from constructor in class " + className,
                    e);
        } catch (IllegalAccessException e) {
            throw new TakmlInitializationException(
                    "Illegal Access, could not create instance from constructor in class " + className, e);
        } catch (IllegalArgumentException e) {
            throw new TakmlInitializationException(
                    "Illegal Argument, could not create instance from constructor in class " + className,
                    e);
        } catch (InvocationTargetException e) {
            throw new TakmlInitializationException(
                    "Invocation Target error, could not create instance from constructor in class "
                            + className,
                    e);
        }
        return mxPlugin;
    }
}
