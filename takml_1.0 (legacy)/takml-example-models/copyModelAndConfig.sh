#!/bin/bash
if [ $# -eq 0 ]
  then
    echo "No arguments supplied, enter directory to copy"
    exit 1
fi
adb shell mkdir /sdcard/com.atakmap.android.takml_framework.plugin
adb shell mkdir /sdcard/com.atakmap.android.takml_framework.plugin/files
adb shell mkdir /sdcard/com.atakmap.android.takml_framework.plugin/files/models
adb shell mkdir /sdcard/com.atakmap.android.takml_framework.plugin/files/configs
adb push $1/* /sdcard/com.atakmap.android.takml_framework.plugin/files/models
adb push $1/*.yaml /sdcard/com.atakmap.android.takml_framework.plugin/files/configs
