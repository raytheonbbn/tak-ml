set -e

cd web-ui
./buildAndCopyReactJs.sh
cd ../

./build.sh

if [ -d "production" ]; then
    echo "Backing up production directory..."
    TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
    mv production "production-$TIMESTAMP"
fi
rm -rf production
mkdir production
mkdir production/logs
cp application/build/libs/takml_server-2.0.jar production
cp application.properties production
cp deployment_files/runProduction.sh production/run.sh
cp deployment_files/createServerContainer.sh production/createServerContainer.sh
cp deployment_files/ReadMeProduction.txt production/ReadMe.txt
cp -r example_models production/
if [ -d "certs" ]; then
    echo "Copying certificates to production folder..."
    cp -r certs production/tak_certs
fi
cp Dockerfile production/Dockerfile
