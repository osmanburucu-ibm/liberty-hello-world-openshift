/**
* Jenkins Doc: https://jenkins.io/doc/
*
**/

openshift.withCluster() {
  env.NAMESPACE = openshift.project()
  env.APP_NAME = "${JOB_NAME}".replaceAll(/-build.*/, '')
  echo "Starting Pipeline for ${APP_NAME}..."
  env.BUILD = "${env.NAMESPACE}"
}

pipeline {
    agent {
      label 'maven'
    }

    stages {
        stage('Maven build') {
          steps {
            sh 'mvn -v'
            sh 'mvn clean package'
          }
        }

        // Run Maven unit tests
        stage('Unit Test'){
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
                openshift.withProject() {
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
        stage ('Scan Container Image') {
        /*
          steps {  
            script {
              def image_digest_arr = IMAGE_DIGEST.split(":")
              def registry = "${env.AQUA_REGISTRY}"

              def scanConfig = [
                registryName: registry,
                workspaceName:"${env.NAMESPACE}",
                imageName: "${env.APP_NAME}@" + image_digest_arr[0],
                imageTag: image_digest_arr[1],
                credentials:"${env.AQUA_CREDENTIALS}"
              ]

              try {
                startImageScan(scanConfig)
                timeout(20) {
                  waitUntil{
                    getImageScanStatus(scanConfig)    
                  }
                }
                getImageScanResults(scanConfig) 
              } catch (err) {
                println "WARNING aqua scan failed! ${err}"
                currentBuild.result = 'UNSTABLE'
              }
            }
          }
              */
        }

        stage ('Push Container Image') {
          /*
            agent {
                label "skopeo"
            }
            steps {  
                script {                    

                    def srcImage = OUTPUT_IMAGE

                    println("Image is now being pushed to https://${env.DST_IMAGE}")

                    openshift.withCluster() {
                        openshift.withProject() {
                          def openshift_token = readFile "/var/run/secrets/kubernetes.io/serviceaccount/token"

                          println "source image: ${srcImage}, dest image: ${env.DST_IMAGE}"

                          withCredentials([usernamePassword(credentialsId: "${env.ARTIFACTORY_CREDENTIALS}", passwordVariable: 'AFpasswd', usernameVariable: 'AFuser')]) {

                                sh """
                                skopeo copy \
                                --src-creds openshift:${openshift_token} \
                                --src-tls-verify=false \
                                --dest-creds ${AFuser}:${AFpasswd} \
                                --dest-tls-verify=false \
                                docker://${srcImage} \
                                docker://${env.DST_IMAGE}
                                """
                                println("Image is successfully pushed to https://${env.DST_IMAGE}")
                            }
                        }
                    }
                }
            }
            */
        }

        stage ('Generate OCP deployment artifacts') {
          steps {
            script {
              sh "mkdir -p ocp_deploy_assets"

              // copy any raw yamls or json from openshift directory to be applied at deploy time
              sh """
              cp openshift/*.yaml ocp_deploy_assets/ || true
              #cp openshift/*.yml ocp_deploy_assets/ || true
              #cp openshift/*.json ocp_deploy_assets/ || true

              #cp -v openshift/*.{yaml,yml,json} ocp_deploy_assets/ || true
              """
              
              // render any deploy templates now and output them to the output directory
              openshift.withCluster() {                    
                openshift.withProject() {
                  files = findFiles(glob: 'openshift/templates/*.*')

                  for (File f : files) {
                    /*
                     * Note: current version of RBC OCP doesn't support `oc process --ignore-unknown-parameters=true`, 
                     * (need version >= 3.7, oc/ocp cli and jenkins plugin?) so we need to have all -p parameters defined in all templates here.
                     */
                    def objects = openshift.process(
                      "-f", "${f.path}", 
                      "-p", "APP_NAME=${env.APP_NAME}",
                      "-p", "IMAGE=${env.DST_IMAGE}",
                      "--ignore-unknown-parameters")

                    //println "Rendered Objects: " + objects

                    dir('ocp_deploy_assets') {
                      for (Object obj : objects) {
                        // write the yaml to files
                        def objectName = obj.metadata.name
                        def objectType = obj.kind
                        writeYaml file: "${objectType}-${objectName}.yaml", data: obj

                        println "Wrote ${objectType}-${objectName}.yaml"
                        sh "cat ${objectType}-${objectName}.yaml"
                      }
                    }
                  }
                }
              }

              // print out the dir structure
              sh "find ocp_deploy_assets"
            }

            /**
             * Checkin deploy configs to GitHub repo? 
             * Process:
             * 1. clone DevOps configs repo to a folder (devops_configs_src)
             * 2. cd in to DevOPs configs repo dir 
             * cp generated ocp compiled template configs to the repo dir 
             * 3. Check in to git: Do a git wizardry -> config, pull latest, commit, push
             **/
             
          }
        }

    }
}
