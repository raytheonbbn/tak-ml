#!/bin/bash

# Check if the correct number of arguments are provided
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <path-to-emm-config-file>"
    exit 1
fi

# Assign the input config file to a variable
EMM_CONFIG_FILE=$1

# Check if the input file exists
if [ ! -f "$EMM_CONFIG_FILE" ]; then
    echo "Error: Config file '$EMM_CONFIG_FILE' not found!"
    exit 1
fi

# Define the target directory on the device
TARGET_DIR="/sdcard/atak/takml/hooks"

# Create the directory structure on the device using adb
echo "Creating directory structure on the device..."
adb shell "mkdir -p $TARGET_DIR"

# Check if the directory was created successfully
if [ $? -ne 0 ]; then
    echo "Error: Failed to create directory on the device!"
    exit 1
fi

# Copy the config file to the target directory using adb
echo "Copying the config file to the device..."
adb push "$EMM_CONFIG_FILE" "$TARGET_DIR/"

# Check if the file was copied successfully
if [ $? -ne 0 ]; then
    echo "Error: Failed to copy the config file to the device!"
    exit 1
fi

echo "Config file successfully copied to $TARGET_DIR."

