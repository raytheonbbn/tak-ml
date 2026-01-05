#!/bin/bash

file="application.properties"
function prop {
    grep "${1}" ${file} | cut -d'=' -f2
}
workingDir=$PWD;

export PGPASSWORD=$(prop 'spring.datasource.password')
echo "SELECT 'CREATE DATABASE takml_server' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'takml_server')\gexec" | psql -d cot -h takserver-db-5.2-RELEASE-56 -U martiuser
./gradlew clean bootRun --args="--spring.config.location=${workingDir}/${file}"
