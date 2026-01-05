See schema.yaml for the Open API Spec.

Make sure to use Java 11 when compiling for TAK ML Android

To generate a Java client that can be used with TAK ML Android, first download SWAGGER Codegen (https://github.com/swagger-api/swagger-codegen?tab=readme-ov-file) via:
```
wget https://repo1.maven.org/maven2/io/swagger/codegen/v3/swagger-codegen-cli/3.0.61/swagger-codegen-cli-3.0.61.jar -O swagger-codegen-cli.jar
```
Then run:
```
java -jar swagger-codegen-cli.jar generate -i schema.yaml -l java --library=okhttp4-gson --api-package com.bbn.takml_server.client --model-package com.bbn.takml_server.client.models -o takml-server-client
```

This will generate an OkHTTP client under the *takml-server-client* folder.

Change to that directory
```
cd takml-server-client
```
At the time of this writing, the codegen tool does not provide the correct dependencies for okhttp3 nor Java 11. Replace your dependencies with:  
```
implementation "io.swagger.core.v3:swagger-annotations:2.1.10"
implementation 'javax.annotation:javax.annotation-api:1.3.2'
implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
implementation 'com.google.code.gson:gson:2.10.1'
implementation 'io.gsonfire:gson-fire:1.8.5'
implementation 'org.threeten:threetenbp:1.6.5'
testImplementation 'junit:junit:4.13.2'

```  
Additionally update the compatibility section in build.gradle with:  
```  
sourceCompatibility = JavaVersion.VERSION_1_8
targetCompatibility = JavaVersion.VERSION_1_8  
```

Update settings.gradle:
```
rootProject.name = "takml-server-client"
```
  
build the project via:
```
chmod a+x gradlew
./gradlew clean build
```