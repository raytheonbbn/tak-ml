Run the buildShadowJarToRoot target.  That will create a jar with everything you need in this directory.

Once you have that you can use cert-packager.sh or cert-packager.bat in either case, the usage is

serverName ip trustStore

     Options:
      --protocol (defaults to ssl)
      --port (defaults to 443)
      --password (defaults to atakatak
      --targetDirectory (defaults to current directory)

Note that if your arguments contain spaces, you may need to put a backslash in front of them.

Example:

./cert-packager.sh ventures-demo 18.253.165.248 /home/nsoule/gitroot/tak-ops/tak-cloud/instances/tak-cloud-demo-server/certs/files/truststore-root.p12 --port=8089 --targetDirectory=/home/nsoule/gitroot/tak-ops/tak-cloud/instances/tak-cloud-demo-server/certs/files/
