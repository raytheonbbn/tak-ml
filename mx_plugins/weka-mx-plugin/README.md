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
./gradlew clean assembleCivDebug

or for Mil:
./gradlew clean assembleMilDebug

Then copy the AAR to your TAK ML MLA plugin

In addition, please include the following dependencies:
```
implementation 'org.tensorflow:tensorflow-lite-task-vision:0.4.0'
implementation 'org.tensorflow:tensorflow-lite-gpu:2.9.0'
implementation 'org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.0'
```
