package ami.build.jenkins.workflow

import com.cloudbees.groovy.cps.NonCPS

class BuildConfig implements Serializable {
    static Map resolve(def body = [:]) {

        // In lieu of closure, Map data structure is used
        // to parse the config block and pass that along
        // to the build job.
        Map config = [:]
        config = body
        if (body in Map) {
            config = body
        } else if (body in Closure) {
            body.resolveStrategy = Closure.DELEGATE_FIRST
            body.delegate = config
            body()
        } else {
            throw  new Exception(sprintf("Unsupported build config type:%s", [config.getClass()]))
        }
        return config
    }
}