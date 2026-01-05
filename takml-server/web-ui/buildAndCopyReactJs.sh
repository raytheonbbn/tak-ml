#!/bin/bash

set -e

npm install
npm run build
mkdir -p ../application/src/main/resources/static
cp -r dist/* ../application/src/main/resources/static/
