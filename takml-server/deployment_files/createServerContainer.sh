docker container create \
    --name tak-ml-server-container \
    -p 127.0.0.1:8234:8234 \
    -v $(pwd)/example_models:/opt/takml_server/example_models \
    -v $(pwd)/logs:/opt/takml_server/logs \
    tak-ml-server
