/**
* Jenkins Doc: https://jenkins.io/doc/
*
**/

openshift.withCluster() {
  env.NAMESPACE = openshift.project()
  env.POM_FILE = env.BUILD_CONTEXT_DIR ? "${env.BUILD_CONTEXT_DIR}/pom.xml" : "pom.xml"
  env.APP_NAME = "${JOB_NAME}".replaceAll(/-build.*/, '')
  echo "Starting Pipeline for ${APP_NAME}..."
  env.BUILD = "${env.NAMESPACE}"
}

pipeline {
    agent any

    stages {
        stage('Maven build') {
          agent {
            label 'maven'
          }

          steps {
            sh 'mvn -v'
            sh 'mvn clean package'
          }
        }

        // Run Maven unit tests
        stage('Unit Test'){
          agent {
            label 'maven'
          }

          steps {
            sh "mvn -B test -f ${POM_FILE}"
          }
        }
    
        // Build Container Image using the artifacts produced in previous stages
        stage('Build Container Image'){
          steps {
            script {
              // Build container image using local Openshift cluster
              // Giving all the artifacts to OpenShift Binary Build
              // This places your artifacts into right location inside your S2I image
              // if the S2I image supports it.
              openshift.withCluster() {
                openshift.withProject(env.BUILD) {
                  timeout (time: 10, unit: 'MINUTES') {
                    // generate the imagestreams and buildconfig
                    def src_image_stream = [
                      "apiVersion": "v1",
                      "kind": "ImageStream",
                      "metadata": [
                        "name": "liberty-base",
                      ],
                      "spec": [
                        "tags": [
                          [
                            "name": "kernel-ubi-min",
                            "from": [
                              "kind": "DockerImage",
                              "name": "ibmcom/websphere-liberty:kernel-ubi-min"
                            ]
                          ]
                        ]
                      ]
                    ]
                    openshift.apply(src_image_stream)

                    def target_image_stream = [
                      "apiVersion": "v1",
                      "kind": "ImageStream",
                      "metadata": [
                          "name": "${env.APP_NAME}",
                      ]
                    ]
                    openshift.apply(target_image_stream)

                    def buildconfig = [
                      "apiVersion": "v1",
                      "kind": "BuildConfig",
                      "metadata": [
                        "name": "${env.APP_NAME}",
                        "namespace": "${env.NAMESPACE}"
                      ],
                      "spec": [
                        "output": [
                          "to": [
                            "kind": "ImageStreamTag",
                            "name": "${env.APP_NAME}:latest"
                          ]
                        ],
                        "source": [
                          "type": "Binary"
                        ],
                        "strategy": [
                          "dockerStrategy": [
                            "from": [
                              "kind": "ImageStreamTag",
                              "namespace": "${env.NAMESPACE}",
                              "name": "liberty-base:kernel-rhel"
                            ],
                            "dockerfilePath": "Dockerfile",
                            "noCache": true,
                            "forcePull": true
                          ]
                        ]
                      ]
                    ]
                    openshift.apply(buildconfig)

                    // run the build and wait for completion
                    def build = openshift.selector("bc", env.APP_NAME).startBuild("--from-dir=.", "--wait")
                    
                    // print the build logs
                    build.logs('-f')

                    // set a global variable for the image digest
                    def buildObj = build.object()
                    def tmpImg = buildObj.status.outputDockerImageReference
                    def imageDigest = buildObj.status.output.to.imageDigest
                    def imgRepoIdx = tmpImg.lastIndexOf(":")
                    println imgRepoIdx
                    //StringBuilder builder = new StringBuilder()
                    OUTPUT_IMAGE = tmpImg.substring(0, imgRepoIdx) + "@" + imageDigest

                    println OUTPUT_IMAGE

                  }
                }        
              }
            }
          }
        }
    }
}
