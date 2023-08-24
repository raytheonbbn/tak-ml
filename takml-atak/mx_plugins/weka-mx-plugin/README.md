TAKML Weka AAR. Supports generic recognition and linear regression with Weka.


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
./gradlew clean assembleCivDebug

or for Mil:
./gradlew clean assembleMilDebug

Then copy the AAR to your TAK ML MLA plugin

Update build.gradle:
```
packagingOptions {
    ...
    exclude 'META-INF/LICENSE.md'
    exclude 'META-INF/NOTICE.md'
    ...
}
...
dependencies {
   ...
   implementation('nz.ac.waikato.cms.weka:weka-stable:3.8.6'){
   exclude group: 'java_cup.runtime'
   exclude group: 'com.github.vbmacher'
   exclude group: 'jakarta.activation'
}
```

For proguard, please use the following rules:
-keep class com.atakmap.android.takml_android.** { *; }
-keep class com.atakmap.android.takml.mx_framework.** { *; }
-keep class weka.** { *; }

This plugin supports the following TakmlResult type:
```
Recognition – For Generic Classification
Regression  – For Linear Regression
```

Code Example:
```
takmlExecutor.executePrediction(bitmapImageBytes, new MXExecuteModelCallback() {
    @Override
    public void modelResult(List<? extends TakmlResult> takmlResults, boolean success, String modelType) {
        for (TakmlResult takmlResult : takmlResults){
            if(modelType.equals(ModelTypeConstants.IMAGE_CLASSIFICATION)){
                Recognition recognition = (Recognition) takmlResult;
                String label = recognition.getLabel();  
                float confidenceScore = recognition.getConfidence();  
            }else{
                // is linear regression
                Regression regression = (Regression) takmlResult;
                float result = regression.getPredictionResult();
            }
        }
    }
});
```