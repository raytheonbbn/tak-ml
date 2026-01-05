#!/bin/bash

file="application.properties"
function prop {
    grep "${1}" ${file} | cut -d'=' -f2
}
workingDir=$PWD;

export PGPASSWORD=$(prop 'spring.datasource.password')
echo "SELECT 'CREATE DATABASE takml_server' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'takml_server')\gexec" | psql -d cot -h localhost -U martiuser
java -jar takml_server-2.0.jar "--spring.config.location=${workingDir}/${file}"
