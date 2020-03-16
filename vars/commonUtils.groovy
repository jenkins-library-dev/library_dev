#!groovy

import jenkins.model.*
import jenkins.branch.BranchIndexingCause
import hudson.triggers.TimerTrigger.TimerTriggerCause
import jenkins.branch.*


def runCmdWithNoOutput(String cmd) {
    /*
        By default Jenkins starts shell scripts with flags -xe. 
        
        -x enables additional logging.
        -e makes the script exit if any command inside returns non-zero exit status. 

        This function retains the functionality associated with '-e', but, turns 
        OFF additional logging by explicitly not including '-x' in the shebang line.
    */
    logProgress.record("runCmdWithNoOutput", "Cmd to execute with output turned OFF, ${cmd}", "log")
    steps.sh (script: '#!/bin/sh -e\n' + cmd, returnStdout: true)
}


def updateEnv(Map values) {

    /*
        Assists in setting up customized environment
        variables that are needed for AMI builds.
    */
    values.each { key, value ->
        env."${key}" = value
    }
}

def getCurrentJobName() {
    jobName = env.JOB_NAME
    return Jenkins.instance.getItemByFullName(jobName).name
}

def getFullJobName() {
    jobName = env.JOB_NAME
    return Jenkins.instance.getItemByFullName(jobName)
}

def isBranchIndexed() {

    def branchIndex = ""

    def jenkinsProjectName = getCurrentJobName()
    logProgress.record("isBranchIndexing", "Determining the build job's cause for project, ${jenkinsProjectName}", "log")

    def jenkinsInstance = jenkins.model.Jenkins.getInstance()
    def jenkinsProject = getFullJobName()

    if (jenkinsProject != null) {
        def job = jenkinsProject.getAllJobs().first()
        logProgress.record("isBranchIndexing", "Scanning jobs of project, ${jenkinsProject}", "log")

        def lastBuild = job.getLastBuild()

        // For build job #1, in the case of new PR, there is __no__ last build!!!
        if ( lastBuild == null ) {
            logProgress.record("isBranchIndexing", "This is the first build of job, ${job}", "log")
            branchIndex = "false"
        } else {
            logProgress.record("isBranchIndexing", "${lastBuild} is the last build of job, ${job}", "log")

            def causes = lastBuild.getCauses()
            logProgress.record("isBranchIndexing", "Getting the causes for the last build, ${causes}", "log")

            def jobCause = causes.first()
            logProgress.record("isBranchIndexing", "Cause for the last build job, ${jobCause}", "log")

            if (jobCause instanceof BranchIndexingCause) {
                branchIndex = "true"
                // Build shall be aborted, but status (of the commit in GHE) shall be forced to
                // 'SUCCESS' b'cos the CI status should not be overwritten (that was set based on
                // the previous runs that could have been triggered either manually or via Branch event(s).
                currentBuild.result = "SUCCESS"
                logProgress.record("isBranchIndexing", "ABORTED: Build job's stages skipped as the job was triggered by branch indexing!!!", "log")
            } else {
                branchIndex = "false"
                logProgress.record("isBranchIndexing", "Branch indexing did _not_ cause this build job to be triggered", "log")
            }
        }
    }

    return branchIndex
}

def buildCause() {

    String build_cause = ""

    for (cause in currentBuild.rawBuild.getCauses()) {
        if (cause instanceof jenkins.branch.BranchEventCause) {
            String scmcause = cause.getShortDescription()
            logProgress.record("buildCause", "Trigger cause, ${scmcause}", "log")
            build_cause = "scm"
            env.BUILD_CAUSE = "SCM Event"
        }

        if (cause instanceof hudson.triggers.TimerTrigger.TimerTriggerCause) {
            if (cause.getShortDescription() == "Started by timer") {
                logProgress.record('buildCause', 'Timer triggered', 'log')
                build_cause = "timer"
                env.BUILD_CAUSE = build_cause.capitalize()
            }
        }

        if (cause.getShortDescription() =~ /Started by upstream.*/) {
            logProgress.record("buildCause",  "Upstream project triggered", "log")
            build_cause = "upstream"
        }

        if (cause instanceof Cause.UserIdCause) {
            String user_id = cause.getUserId().trim()
            logProgress.record("buildCause", "Triggered manually by ${user_id}", "log")
            build_cause = "manual"
            env.BUILD_CAUSE = "OnDemand"
            env.USER_ID = user_id
        }
    }

    return build_cause
}

def abortPreviousRunningBuilds() {
    // Detect active build jobs for 'this_job', and,
    // abort them, if new one is in the Queue.
    def jobname = ""
    def build_number = ""
    def this_job = ""

    jobname = "${env.JOB_NAME}"
    build_number = env.BUILD_NUMBER.toInteger()
    this_job = Jenkins.instance.getItemByFullName(jobname)

    logProgress.record("abortPreviousRunningBuilds", "Current build job is ${this_job} and the build #, ${build_number}", 'log')
    for (def build : this_job.builds) {
        if ( build.isBuilding() && build.number.toInteger() != build_number ) {
            def abortjob_buildnumber = build.number.toInteger()
            logProgress.record("abortPreviousRunningBuilds", " =============> Cancelling build job # ${abortjob_buildnumber} <=============", 'log')
            build.doStop()
        }
    }
}

def setToolsEnv() {
    env.UNZIPCLI = sh(returnStdout: true,
                        script: '''
                                which unzip 2> /dev/null
                                '''
                    ).trim()
    env.JQCLI = sh(returnStdout: true,
                    script: '''
                            which jq 2> /dev/null
                            '''
                    ).trim()
}

return this;
