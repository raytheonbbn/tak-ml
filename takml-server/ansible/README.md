# **Ansible Setup Guide**

This guide provides step-by-step instructions for configuring and installing TAK ML Server and TAK Server via Ansible.

---

## **1. Installing Ansible on Linux Machines**

### Tested Distributions
- **RHEL 8**
- **CentOS Stream 9**
- **Fedora 40/41**

#### **Prerequisites for RHEL 8 and CentOS**
1. Ensure you have `epel-release` repository enabled:
   ```bash
   sudo yum install epel-release -y
### **Steps for Installation**
1. Install Ansible
    ```bash
    sudo dnf install ansible -y
2. Verify installation
   ```bash
   ansible --version
## **2. Configuration**
### **Host Configuration**
There are 2 services: 
1. **TAK Server Service**: Tactical Assault Kit / Team Awareness Kit Server
2. **TAK ML Server**: 

The hosts (e.g. `takserver_host`) are configured via the `inventory.ini` file, which is structured with the following configurable parameters:
* **ansible_host**: ip address for connectivity to host
* **ansible_user**: the user to use to connect to host
* **ansible_password**: password for ssh connection to host (a public key will be copied over during the script for passwordless-connection)
* **ansible_become_password**: password to become sudo user on host, typically same as the `ansible_password`
* **ansible_port**: port for connection to host

Each host has an associated `vars` config (e.g. `takserver_host:vars`), which are custom config parameters for that host. These are desctri

`[takserver_host:vars]`
* **takserver_folder_location**: path to the TAK Server Docker folder
* **cot_routing_folder_location**: path to the CoT Routing TAK Server plugin folder
* Certificate Metadata for TAK Server
    * **cert_metadata_country**
    * **cert_metadata_state**
    * **cert_metadata_city**
    * **cert_metadata_organization**
    * **cert_metadata_organization_unit**
* **num_client_certs_generate**: number of client certificates to generate. Each client will be named 'art_user_x' (e.g. 'art_user_1', 'art_user_2', etc.)

`[takml_server_host:vars]`
* **takml_server_folder_location**: path to folder containing TAK ML Server Jar and application.properties
* **takml_server_port**: port for takml server to bind to
* **server_ssl_key-store**: client certificate to use (e.g. takserver.jks) 
* **server_ssl_key-store-password**: client password (e.g. atakatak)
* **server_ssl_key-store-type**: type (e.g. PKCS12)
* **server_ssl_trust-store**: trust store certificate to use (e.g. truststore-root.jks)
* **server_ssl_trust-store-password**: trust store password (e.g. atakatak)
* **server_ssl_trust-store-type**: type (e.g. PKCS12)
* **tak_addr**: ip for TAK Server
* **tak_port**: port for TAK Server (e.g. 8443)
* **tak_client_store**: TAK Server client certificate to use (e.g. takserver.jks) 
* **tak_client_store.password**: client password (e.g. atakatak)
* **tak_trust_store**: TAK Server trust store certificate to use (e.g. truststore-root.jks)
* **tak_trust_store.password**: trust store password (e.g. atakatak)
* **models_directory**: directory where models are located

`[all:vars]`
* **docker_repo**: Docker repo (e.g. for CentOS: `https://download.docker.com/linux/centos/docker-ce.repo`)

### **TAKML Server Configuration - application.properties**

The contents are described below

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
tak.client_store=/path/to/tak/client/keystore (e.g. takserver.jks) ## Note this is the path inside the container itself
tak.client_store.password=[atakatak]
tak.trust_store=/path/to/takserver/truststore (e.g. truststore-root.jks) ## Note this is the path inside the container itself
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

## **3. Installation**
To install, simply run:
```bash
ansible-playbook playbook.yml -i inventory.ini --ask-become-pass
```

This process may take 10+ minutes, possibly up to an hour on slower networks.

You can check the status via tailing the ART log:
1. SSH to the TAK ML Server machine
2. Tail the Reporting Service log:
    ```bash
    tail -f /opt/takml_server/takml_server/logs/takml_server.log