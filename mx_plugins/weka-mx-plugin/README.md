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

(No additional dependencies)

For proguard, please use the following rules:
-keep class com.atakmap.android.takml_android.** { *; }
-keep class com.atakmap.android.takml.mx_framework.** { *; }
-keep class weka.** { *; }
