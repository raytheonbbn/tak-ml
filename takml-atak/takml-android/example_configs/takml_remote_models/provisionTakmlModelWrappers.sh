#!/bin/bash

# Check if the folder argument is provided
if [ -z "$1" ]; then
  echo "Usage: $0 <source_folder>"
  exit 1
fi

# Assign the source folder to a variable
SOURCE_FOLDER=$1

# Check if the source folder exists
if [ ! -d "$SOURCE_FOLDER" ]; then
  echo "Error: Source folder '$SOURCE_FOLDER' does not exist."
  exit 1
fi

# Define the target directory on the Android device
TARGET_DIR="/sdcard/atak/takml/models"

# Create the target directory and its parent directories if they don't exist
adb shell "mkdir -p $TARGET_DIR"

# Push the contents of the source folder to the target directory
adb push "$SOURCE_FOLDER" "$TARGET_DIR/"

# Check if the push was successful
if [ $? -eq 0 ]; then
  echo "Contents of '$SOURCE_FOLDER' successfully pushed to '$TARGET_DIR'."
else
  echo "Error: Failed to push contents to '$TARGET_DIR'."
  exit 1
fi