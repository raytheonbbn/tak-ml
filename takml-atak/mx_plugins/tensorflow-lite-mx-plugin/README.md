TAKML Tflite AAR. Supports image classification and object detection with Tensorflow Lite.


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

In addition, please include the following dependencies:
```
implementation 'org.tensorflow:tensorflow-lite-task-vision:0.4.0'
implementation 'org.tensorflow:tensorflow-lite-gpu:2.9.0'
implementation 'org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.0'
```

For Proguard, add the following rules:
-keep class com.atakmap.android.takml_android.** { *; }
-keep class com.atakmap.android.takml.mx_framework.** { *; }
-keep class org.tensorflow.lite.** { *; }

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