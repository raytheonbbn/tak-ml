# TAK-ML MX Server

This is the server side component for the Model Execution (MX) server of TAK-ML. This component will perform server-side model execution and allow loading of models and plugins from the server to the client.

# Setup
## Installation
To install `tak-ml`, use the `scripts/setup.sh` script from within the either the `takml-roger/mxf` or `takml-roger/sf` directory:

    user@host:/tak-ml/server-side/takml-roger/mxf$ ./scripts/setup.sh

This script installs the required dependencies, builds `takml-roger`, and creates any necessary global files. This includes creating `/opt/takml/models/`, which is where `takml-roger` looks for binary models used by plugins.

## Configuring and Running
Once installed, `takml-roger` can be run as follows:

    user@host:/tak-ml/server-side/takml-roger/mxf$ ./scripts/run.sh

The `run.sh` script points to a ROGER configuration file. By default, the configuration file is `config/mxf/mxf_config.json`. Configuration files and unit tests make assumptions about pathnames for models and input data that should be changed depending on your deployment.

# Development
## Framework Development
To load takml-roger into Eclipse:
1. Build `takml-roger` using the build script (this will generate the protobuf code).
    user@host:/tak-ml/server-side/takml-roger$ ./scripts/build.sh
2. Import `takml-roger` as a gradle project
3. **Link** the generated code. To do so, right click on the takml-roger project in Eclipse --> Configure Build Path --> Link Source. There are two folders to link:
        Navigate to takml-roger/build/generated/source/proto/main/java for the folder location, and use "com" as the folder name.
        Navigate to takml-roger/build/generated/source/proto/main/grpc for the folder location, and use "grpc" as the folder name.
4. While looking at the Java Build Path, "takml-roger/build/swagger/src/main/java" should be included as source, as well. This happened automatically upon importing the gradle project, but your milage may vary. Ensure that it is listed as source.

