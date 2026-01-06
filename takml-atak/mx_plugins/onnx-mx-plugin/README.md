TAKML Onnx AAR. Supports Image Classification with Onnx.


_________________________________________________________________
PURPOSE AND CAPABILITIES

(General Description)


_________________________________________________________________
STATUS

Beta

_________________________________________________________________
POINT OF CONTACTS

Brandon Kalashian - brandon.kalashian@rtx.com
Nate Soule - nathaniel.soule@rtx.com

_________________________________________________________________
PORTS REQUIRED

NA

_________________________________________________________________
EQUIPMENT REQUIRED

NA

_________________________________________________________________
EQUIPMENT SUPPORTED

NA

_________________________________________________________________
COMPILATION

NA

_________________________________________________________________
DEVELOPER NOTES

To build, please run:
./build_aar.sh
(or ./gradlew clean assembleCivDebug)

or for Mil:
./build_aar.sh -mil
(or ./gradlew clean assembleMilDebug)

Then copy the AAR to your TAK ML MLA plugin

Update build.gradle:
```
packagingOptions {
    ...
    exclude 'META-INF/LICENSE.md'
    exclude 'META-INF/NOTICE.md'
    ...
    
    dependencies{
        ...
        implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.18.0'
    }
}
```

For proguard, please use the following rules:
-keep class com.atakmap.android.takml_android.** { *; }
-keep class com.atakmap.android.takml.mx_framework.** { *; }
-keep class ai.onnxruntime.** { *; }

This plugin supports the following TakmlResult type:
```
Recognition – For Image Classification results
```

Code Example:
```
takmlExecutor.executePrediction(bitmapImageBytes, new MXExecuteModelCallback() {
    @Override
    public void modelResult(List<? extends TakmlResult> takmlResults, boolean success, String modelType) {
        for (TakmlResult takmlResult : takmlResults){
            Recognition recognition = (Recognition) takmlResult;
            String label = recognition.getLabel();  
            float confidenceScore = recognition.getConfidence();  
        }
    }
});
```

Note: the Onnx Mx Plugin will use the following model shape by default:
1, 3, 224, 224

To use a different shape, you will need to define a `com.atakmap.android.takml.mx_framework.onnx_plugin.OnnxProcessingParams`
and set it via:
```
new TakmlModel.TakmlModelBuilder(...).setProcessingParams(onnxProcessingParams)
```
The OnnxProcessingParams takes in a float[] array, so for 1, 3, 224, 224:
```
float[] modelShape = new long[]{1, 3, 224, 224};
```
and a pixel x width and pixel y height. 
Collectively, this might look like:
```
new TakmlModel.TakmlModelBuilder()
...
.setProcessingParams(new OnnxProcessingParams(new long[]{1, 512, 512, 3},
512, 512))
```

You can also embed a GSON serialized `OnnxProcessingParams` in a TAKML Model Zip file, and point to it in the takml_config.yaml via
`processingConfig: [yourSerializedOnnxProcessingParamsFile.json]`