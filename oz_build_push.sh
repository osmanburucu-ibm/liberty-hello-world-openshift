#!/bin/bash
echo "Liberty Hello World to OpenShift using UrbanCode Deploy"

BUILD_VERSION=0.0.0

echo "Parameters are:"
while true; do
    case $1 in 
        -v | --version )
            BUILD_VERSION=$2; echo "Version for new build=$BUILD_VERSION"; shift 2 ;;
        -- ) echo "-- $1"; shift; break ;;
        * ) break ;;
    esac
done

if [[ "$BUILD_VERSION" == "0.0.0" ]]; 
then 
    echo "Please enter new Version with -v parameter"
    exit 1 
fi 

# first step build with maven
# mvn clean install

# second step build container image, need input variable for version
docker build . -t demowltp:"${BUILD_VERSION}"
# TODO: first check/test that this version works, then use gh/gitlab cli to create new release on repo, push new version to ucd component!
docker tag demowltp:"${BUILD_VERSION}" quay.io/osmanburucuibm/demowltp:"${BUILD_VERSION}"

docker push quay.io/osmanburucuibm/demowltp:"${BUILD_VERSION}"