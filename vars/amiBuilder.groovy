#!groovy

import ami.build.jenkins.workflow.BuildConfig

def call(def body = [:]) {

    def postBuildActionCallback
    def triggers = []

    // https://stackoverflow.com/questions/54124966/passing-parameters-from-jenkinsfile-to-a-shared-library
    // Allows global properties to be override in the child jobs
    config = BuildConfig.resolve(body)

    if ( config.get('post_build_steps') != null ) {
        postBuildActionCallback = config.get("post_build_steps")
    }

    properties([
        disableConcurrentBuilds(),
        buildDiscarder(logRotator(numToKeepStr: '10')),
        parameters([
            string(defaultValue: "", description: "Default is delta9.", name: 'BUILD_ACCOUNT', trim: false)
        ]),
        pipelineTriggers(triggers)
    ])

    timeout(time: 15, unit: 'MINUTES') {
        node("master") {
            timestamps {
                ansiColor('xterm') {
                        stage('job-init') {
                            progressLogger.record('amiBuilder', 'Cleaning up workspace before building', "INFO")
                            deleteDir()
                        }

                        try {
                            sh 'printenv'
                            progressLogger.record("amiBuilder", "AMI build passed, check the build console output!", "INFO")
                        } catch (Exception error) {
                            progressLogger.record("amiBuilder", "AMI build failed, check the build console output!", "ERROR")
                        } finally {
                            if (postBuildActionCallback != null) {
                                postBuildActionCallback.call()
                            }
                            stepCleanup()
                        }
                }
            }
        }
    }
}

return this;
