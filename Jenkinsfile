/**
* Jenkins Doc: https://jenkins.io/doc/
*
**/

openshift.withCluster() {
  env.NAMESPACE = openshift.project()
  env.APP_NAME = "${JOB_NAME}".replaceAll(/-build.*/, '')
  echo "Starting Pipeline for ${APP_NAME}..."
  env.BUILD = "${env.NAMESPACE}"
  env.DEPLOY_REPO_URL = "git@github.com:jkwong888/liberty-hello-world-openshift-deploy.git"
  env.DEPLOY_REPO_BRANCH = "master"
  env.DEPLOY_REPO_CREDS = "github-deploy-key"

  env.EXTERNAL_IMAGE_REPO_URL = "harbor.jkwong.cloudns.cx"
  env.EXTERNAL_IMAGE_REPO_NAMESPACE = "jkwong-pub"
  env.EXTERNAL_IMAGE_REPO_CREDENTIALS = "harbor"
  env.DST_IMAGE = "${env.EXTERNAL_IMAGE_REPO_URL}/${env.EXTERNAL_IMAGE_REPO_NAMESPACE}/${env.APP_NAME}:${env.BUILD_NUMBER}"
}

pipeline {
    agent {
      label "maven"
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
            sh "/opt/rh/rh-maven35/root/usr/bin/mvn -B test"
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

                    OUTPUT_IMAGE = tmpImg.substring(0, imgRepoIdx) + "@" + imageDigest

                    println OUTPUT_IMAGE

                  }
                }        
              }
            }
          }
        }
        stage ('Scan Container Image') {
          steps {  
            script {
              println "scan container image"
        /*
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
              */
            }
          }
        }

        stage ('Push Container Image') {
          agent {
            kubernetes {
              cloud 'openshift'
              label 'skopeo-jenkins'
              yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: jnlp
    image: jkwong/skopeo-jenkins 
    tty: true
  serviceAccountName: jenkins
"""
            }
          }

          steps {  
              script {                    
                  def srcImage = OUTPUT_IMAGE

                  openshift.withCluster() {
                      openshift.withProject() {
                        def openshift_token = readFile "/var/run/secrets/kubernetes.io/serviceaccount/token"

                        println "source image: ${srcImage}, dest image: ${env.DST_IMAGE}"

                        withCredentials([usernamePassword(credentialsId: "${env.EXTERNAL_IMAGE_REPO_CREDENTIALS}", passwordVariable: 'AFpasswd', usernameVariable: 'AFuser')]) {
                              sh """
                              /usr/bin/skopeo copy \
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


        stage ('Push deployment artifacts') {
          steps {
            sh "mkdir -p deploy"

            dir ('deploy') {
              git url: "${env.DEPLOY_REPO_URL}",
                  branch: "${env.DEPLOY_REPO_BRANCH}",
                  credentialsId: "${env.DEPLOY_REPO_CREDS}"

              sh "find ."

              withCredentials([sshUserPrivateKey(credentialsId: "${env.DEPLOY_REPO_CREDS}", 
                                                 keyFileVariable: 'SSH_KEY')]) {
                sh """
                cp ../ocp_deploy_assets/* .
                git config user.email "jenkins@jenkins.com"
                git config user.name "jenkins"
                git add .
                git commit -m "jenkins commit ${env.BUILD_NUMBER}"
                """

                writeFile file: "local_ssh.sh",
                     text: """
                     ssh -i ${env.SSH_KEY} -l git -o StrictHostKeyChecking=no "\$@"
                     """

                sh 'chmod +x local_ssh.sh'
                withEnv(['GIT_SSH=./local_ssh.sh']) {
                    sh "git push origin ${env.DEPLOY_REPO_BRANCH}"
                }
              }
            }
          }
        }
    }
}
