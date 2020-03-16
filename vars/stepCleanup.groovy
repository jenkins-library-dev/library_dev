#!groovy

def call() {
    stage("Clean Workspace == ${env.NODE_NAME}") {
        cleanWorkspace()
        progressLogger.record("cleanup", "build workspace cleanup done!!!", "INFO")
    }
}

def cleanWorkspace() {
    // clean workspace dir
    def build_workspace = pwd()

    dir("${build_workspace}@tmp") {
        progressLogger.record("cleanupJob", "Cleaning tmp folder in ${build_workspace}", "INFO")
        deleteDir()
    }

    dir("${build_workspace}") {
        progressLogger.record("cleanupJob", "Cleaning workspace folder in ${build_workspace}", "INFO")
        deleteDir()
    }
}
