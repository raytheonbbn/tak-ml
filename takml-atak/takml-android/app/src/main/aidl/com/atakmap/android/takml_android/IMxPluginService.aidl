// IMxPluginService.aidl
package com.atakmap.android.takml_android;

interface IMxPluginService {
    void registerModel(String modelUUID, String name, String modelExtension, String modelType, String modelFilePath,
    String serializedProcessingParams, in List<String> labels);
    void execute(String requestUUID, String modelUUID, in byte[] inputDataBitmap);
    void executeTensor(String requestUUID, String modelUUID, in ParcelFileDescriptor pd);
}