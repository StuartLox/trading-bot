#!/bin/sh -e

echo "Build application"
./gradlew --stacktrace clean build
