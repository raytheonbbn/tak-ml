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
./gradlew clean assembleCivDebug

or for Mil:
./gradlew clean assembleMilDebug

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
