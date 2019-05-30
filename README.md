# Java Hello World Sample

This project contains a simple servlet application.

## Docker build

The server.xml and runtime-vars.xml in src/main/wlp are for the Docker image.

```
mvn clean install
docker rm -f hello-world
docker build -t hello-world .
docker run -d -p 9080:9080 --name hello-world hello-world
docker exec -it hello-world /bin/bash
```

## Manual CF deploy

This works on IBM Clould CF by manually pushing the defaultServer directory, not via the pipeline.

````
mvn clean install
cp target/JavaHelloWorldApp.war defaultServer/apps
cf push
```

The app uses user-provided services, which can be created as follows:

```
cf cups ups1 -p '{ "key1": "value1" }'
cf cups ups2 -p '{ "key2": "value2" }'
cf cups ups3 -p '{ "key3a": "value3a", "key3b": "value3b" }'
```

[![Deploy to Bluemix](https://bluemix.net/deploy/button.png)](https://bluemix.net/deploy?repository=https://github.com/IBM-Bluemix/java-helloworld)

## Running the application using the command-line

This project can be built with [Apache Maven](http://maven.apache.org/). The project uses [Liberty Maven Plug-in][] to automatically download and install Liberty from the [Liberty repository](https://developer.ibm.com/wasdev/downloads/). Liberty Maven Plug-in is also used to create, configure, and run the application on the Liberty server.

Use the following steps to run the application locally:

1. Execute full Maven build to create the `target/JavaHelloWorldApp.war` file:

   ```bash
   $ mvn clean install
````

2. Download and install Liberty, then use it to run the built application from step 1:

   ```bash
   $ mvn liberty:run-server
   ```

   Once the server is running, the application will be available under [http://localhost:9080](http://localhost:9080).

Use the following command to run the built application in Bluemix:
`bash $ cf push <appname> -p target/JavaHelloWorldApp.war`

[Example app instance](https://hello-world.mybluemix.net/)

## Manual OC deploy

Steps to deploy our docker image to OCP using a slightly modified version of the deployment-template from Doug.

#### push app docker image to the intermediate repo (e.g. artifactory)

```
  docker tag hello-world:latest harbor.jkwong.cloudns.cx/togarage/hello-world:latest
  docker push harbor.jkwong.cloudns.cx/togarage/hello-world:latest
```

#### set up OC CLI in a container on pairing station

```
  alias oc="docker run --rm -i -v $HOME/.kube:/root/.kube -v `pwd`:/tmp -w /tmp --entrypoint oc openshift/origin-cli:v3.11"
```

#### log into OCP using token from UI console

```
  oc login https://ocp-prod.ibm-gse.jkwong.xyz:443 --token=xxxxx
```

#### create deployment template on OCP from yaml

```
  oc create -f deploy-template.yaml
  oc apply -f deploy-template.yaml
```

#### set up secret to allow deployment to pull from OCP image registry

```
  oc create secret docker-registry togarage-pull-secret --docker-server=harbor.jkwong.cloudns.cx --docker-username=shili --docker-password=Letmein123 --docker-email=shiliy@ca.ibm.com
  oc secrets link default togarage-pull-secret --for=pull
```

#### Deploy the app using the deployment-template

```
  oc new-app --template=dc-helloworld-template --param APP_NAME=hello-world --param TAG=latest --param APP_DC_NAME=hello-world --param APP_ARTIFACT_ID=hello-world --param TARGET_REPO=harbor.jkwong.cloudns.cx --param TARGET_WORKSPACE=togarage
  oc rollout latest hello-world

```

The app is [running here](https://hello-world-liberty.app-ocp-prod.ibm-gse.jkwong.xyz/)

#### (Optional) clean up a app

```
  oc delete dc/hello-world
  oc delete service hello-world
  oc delete route hello-world
```
