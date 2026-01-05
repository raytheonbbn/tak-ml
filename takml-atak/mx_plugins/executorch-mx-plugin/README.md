TAKML Pytorch AAR. Supports Torchscript based Pytorch Image Classification and Object Detection.


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

Then copy the AAR to your TAK ML MLA plugin. 

In addition, please include the following dependencies:
```
implementation group: 'org.apache.commons', name: 'commons-lang3', version: '3.12.0'
implementation('org.pytorch:pytorch_android_lite:1.13.0'){
exclude group : 'androidx.*'
}
implementation('org.pytorch:pytorch_android_torchvision_lite:1.13.0'){
exclude group : 'androidx.*'
}
```

If using Proguard, add the following:
-keep class com.atakmap.android.takml_android.** { *; }
-keep class com.atakmap.android.takml.mx_framework.** { *; }
-keep class org.pytorch.** { *; }
-keep class com.facebook.jni.** { *; }

This plugin supports the following TakmlResult type:
```
Recognition – For Image Classification and Object Detection results
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
            
            if(modelType.equals(ModelTypeConstants.OBJECT_DETECTION)){
                 float leftImageCoord = recognition.getLeft();
                 float rightImageCoord = recognition.getRight();
                 float topImageCoord = recognition.getRight();
                 float bottomImageCoord = recognition.getBottom();
            }else{
                // is image classification
            }
        }
    }
});
```