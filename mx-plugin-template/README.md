TAK ML MX Plugin Template


_________________________________________________________________
PURPOSE AND CAPABILITIES

Library to assist running machine learning toolkits and applications in ATAK.


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

For example, place the AAR in app/libs and in build.gradle:
implementation files("libs/[name of your aar].aar")

The TAK ML AAR is a required dependency for MLA and MX plugins. See the Quick Start Guide for additional help.

For proguard, please use the following rules:
-keep class com.atakmap.android.takml_android.** { *; }
-keep class com.atakmap.android.takml.mx_framework.** { *; }