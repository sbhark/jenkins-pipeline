def call(body) {
    // Get paramters defined in Jenkinsfile if any
    Map config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    // Set properties to variables, so you dont need to call config on every call
    def nodeLabel = config.nodeLabel != null ? config.nodeLabel : "master"
    def buildTimeout = config.buildTimeout != null ? config.buildTimeout : 15
    def projectName = config.projectName

    node(nodeLabel) {
        pipeline {
            timestamps {
                def workspace = pwd()
                def gitBranch = env.BRANCH_NAME
                def buildNumber = env.BUILD_NUMBER
                def packageName = "${projectName}-${gitBranch}-${buildNumber}.zip"

                stages {
                    stage("Checkout ${projectName} from SCM") {
                        checkout scm
                    }

                    stage('Building project') {
                        timeout(time: buildTimeout, unit: 'MINUTES') {
                            steps {
                                sh 'mkdir build'
                                dir('build') {
                                    sh 'cmake ..'
                                    sh 'make'
                                }
                                zip(zipFile: "${packageName}", dir: "${workspace}/build/${projectName}")
                            }
                        }
                    }

                    stage('Running tests') {

                    }

                    stage('Publishing to artifactory') {
                        def packageZip = "${workspace}/${packageName}"
                        def artifactoryUploadSpec =
                                """
                            {
                              "files": [
                                {
                                  "pattern": "${executable}",
                                  "target": "generic-local"
                                }
                              ]
                            }
                        """
                        def uploadToArtifactory = artifactoryServer.upload spec: artifactoryUploadSpec
                    }
                }
            }
        }
    }
}
