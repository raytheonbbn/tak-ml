For Ansible install automation, see the ansible/ReadMe.md

1. Make sure TAK Server is installed and running (tested with 5.4 and 5.5).
2. Install Postgresql (e.g. on Ubuntu: sudo apt install postgresql postgresql-contrib)
3. Update application.properties and point to correct TAK Certs
4. run ./buildProduction.sh
5. shadowJar with config will be generated under 'production' folder. This folder contains everything needed to run takml.
6. cd into production and run ./run.sh to run TAK ML

The application.properties config file is explained below:

```apacheconf
server.port=8234
logging.level.org.springframework.web=[DEBUG]
logging.level.com.myCompany=[INFO]
security.require-ssl=[true]
## Enable mutual authentication with PKCS12
server.ssl.enabled=[true]
server.ssl.key-store-password=[atakatak]
server.ssl.key-store=/path/to/client/keystore (e.g. takserver.jks) ## Note this is the path inside the container itself
server.ssl.key-store-type=[PKCS12]
server.ssl.trust-store=/path/to/truststore (e.g. truststore-root.jks) ## Note this is the path inside the container itself
server.ssl.trust-store-password=[atakatak]
server.ssl.trust-store-type=[PKCS12]
client-auth=want
## Set an API Key if desired, comment out 'server.ssl.client-auth' above to disable mutual auth / PKCS12
server.api.key.param_name=my_test_key_name
server.api.key=my_fake_test_key_value
server.api.key.enabled=false
# SpringDoc OpenAPI configuration
springdoc.api-docs.path=[/v3/api-docs]
springdoc.swagger-ui.path=[/swagger-ui.html]
# Database
spring.datasource.url=[jdbc:postgresql://localhost:5432/takml_server]
spring.datasource.username=[martiuser]
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=[update]
spring.jpa.properties.hibernate.dialect=[org.hibernate.dialect.PostgreSQLDialect]
spring.servlet.multipart.max-file-size=[1GB]
spring.servlet.multipart.max-request-size=[1GB]
# TAK Server
tak.addr=localhost
tak.port=8443
tak.client_store=/path/to/tak/client/keystore (e.g. takserver.jks) ## If running in Docker, note this is the path inside the container itself
tak.client_store.password=[atakatak]
tak.trust_store=/path/to/takserver/truststore (e.g. truststore-root.jks) ## If running in Docker, Note this is the path inside the container itself
tak.trust_store.password=[atakatak]
# TAK FS
takfs.mission_name=[takml_takfs_sync_mission]
takfs.takserver.file_limit_size.mb=[500]
takfs.segmented_file_upload_timeout_millis=[10000]
# Mx Plugins
mx_plugins=[com.bbn.takml_server.model_execution.mx_plugins.OnnxMxPlugin,com.bbn.takml_server.model_execution.mx_plugins.PytorchMxPlugin]
# Models
models_directory=/path/to/your/models (e.g. takml-server/example_models)
```
