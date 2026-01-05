TAK ML AAR


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
./build_aar.sh
(or ./gradlew clean assembleCivDebug)

or for Mil:
./build_aar.sh -mil
(or ./gradlew clean assembleMilDebug)

Then copy the AAR to your TAK ML MLA plugin or MX Plugin.

For example, place the AAR in app/libs and in build.gradle:
implementation files("libs/takml-android.aar")

Then add the following dependencies:
// TAK ML Dependencies
implementation "com.squareup.retrofit2:retrofit:2.7.1"
implementation "com.squareup.retrofit2:converter-scalars:2.7.1"
implementation "com.squareup.retrofit2:converter-gson:2.7.1"
implementation "io.swagger.core.v3:swagger-annotations:2.2.15"
implementation "org.json:json:20180130"
implementation "io.gsonfire:gson-fire:1.8.0"
implementation "org.threeten:threetenbp:1.3.5"
implementation group: 'javax.annotation', name: 'javax.annotation-api', version: '1.3.2'
implementation 'com.squareup.okhttp3:okhttp:4.9.2'
implementation group: 'org.apache.logging.log4j', name: 'log4j-api', version: '2.21.1'
implementation 'org.slf4j:slf4j-api:1.7.25'
implementation group: 'commons-io', name: 'commons-io', version: '2.11.0'
implementation 'com.celeral:log4j2-android:1.0.0'
implementation 'org.yaml:snakeyaml:1.33'

The TAK ML AAR is a required dependency for MLA and MX plugins. See the guides at the root of this 
repo for additional help.

For proguard, please use the following rules:
#############################################
# TAK ML + Core Modules
#############################################
-keep class com.bbn.takml_server.** { *; }
-keep class com.atakmap.android.takml_android.** { *; }
-keep class com.atakmap.android.takml.mx_framework.** { *; }

#############################################
# Watchtower DPC (optional dependency)
#############################################
-keep class us.watchtower.dpc.** { *; }
-dontwarn us.watchtower.dpc.**

#############################################
# OkHttp / Okio / SLF4J
#############################################
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okhttp3.logging.** { *; }
-keep interface okhttp3.logging.** { *; }
-keep enum okhttp3.logging.HttpLoggingInterceptor$Level { *; }

-dontwarn okio.**
-dontwarn com.squareup.okhttp.**
-dontwarn okhttp3.**
-dontwarn org.slf4j.impl.**

-keep class org.slf4j.impl.** { *; }
-keep class org.slf4j.helpers.** { *; }
-keep class org.slf4j.spi.** { *; }

#############################################
# TIFFBitmapFactory JNI Bridge
#############################################
-keep class org.beyka.tiffbitmapfactory.** { *; }
-keepclassmembers class org.beyka.tiffbitmapfactory.** {
native <methods>;
}
-keep class org.beyka.tiffbitmapfactory.TiffBitmapFactory { *; }

#############################################
# Miscellaneous / Noise Suppression
#############################################
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn kotlin.**
-dontwarn com.google.protobuf.**