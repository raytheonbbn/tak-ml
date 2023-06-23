#!/bin/bash

# Build the plugin manifest first so that it is included in the .jar.
./gradlew clean
./gradlew test --tests *PluginManifestGenerator
./gradlew shadowJar build -x test

# Assume location of takml-roger.
cp build/libs/takml-roger-mxp-*-all.jar ../../takml-roger/mxf/plugins/
