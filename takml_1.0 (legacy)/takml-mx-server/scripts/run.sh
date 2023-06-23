#!/bin/bash

java -Dlogback.configurationFile=config/mxf/logback.xml -cp libs/roger-core-fullRelease-all-1.2.2-SNAPSHOT.jar com.bbn.roger.launcher.CommandLineLauncher --configFile config/mxf/mx_config.json
