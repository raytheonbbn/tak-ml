set -e
./gradlew clean build -x test
./gradlew :docs:exportOpenApi