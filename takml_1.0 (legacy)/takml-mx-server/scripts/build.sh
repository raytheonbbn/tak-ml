#!/bin/bash

# Build the plugin manifest first so that it is included in the .jar.
./gradlew clean
./gradlew test --tests *PluginManifestGenerator
./gradlew shadowJar build publish -x test
cp build/libs/takml-mx-*-all.jar plugins/
