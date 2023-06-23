#!/bin/bash

# Get dependencies.
apt-get update
apt-get install -y openjdk-8-jre-headless openjdk-8-jdk-headless python3 python3-pip

# Get MISTK.
wget https://github.com/mistkml/mistk/releases/download/0.4.10/mistk-0.4.10-py3-none-any.whl
wget https://github.com/mistkml/mistk/releases/download/0.4.10/mistk_test_harness-0.4.10-py3-none-any.whl
pip3 install mistk-*-py3-none-any.whl
pip3 install pandas  # for MISTK example
pip3 install sklearn # for MISTK example

mkdir plugins

# Build TAK-ML.
./scripts/build.sh

# Create default models and metadata directories.
mkdir -p /opt/takml/models
mkdir -p /opt/takml/metadata
