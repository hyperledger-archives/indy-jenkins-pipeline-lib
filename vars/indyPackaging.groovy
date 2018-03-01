import groovy.transform.Field
@Field String logPrefix = "indyPackaging"
@Field def logger = indyLogger

def call(body) {
    stage('Packaging') {
        def indyConfig = indyConfig()
        // TODO stash if node is null
        config = [
            node: indyConfig.indyNodeLabel, // null is expectable here
            builders: [:],
            version: null
        ]

        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()

        logger.info("parameters: $config", logPrefix)

        def err = null
        if (!config.version) {
            err = "version is not defined"
        } else if (!config.builders) {
            err = "no builders defined, nothing to do"
        }

        if (err) {
            logger.error(err, logPrefix)
            error err
        }

        if (config.node) {
            indyJNode(config.node) {
                return doPackaging(config.builders, config.version, indyConfig.failFast)
            }
        } else {
            // 'node' is expected to be defined in outer scope
            return doPackaging(config.builders, config.version, indyConfig.failFast)
        }
    }
}

def doPackaging(builders, version, failFast) {
    logger.info("Checkout csm", logPrefix)
    checkout scm

    def sourcePath = sh(returnStdout: true, script: 'readlink -f .').trim()

    def builds = [:]
    def volumesOrPaths = [:]
    def _builders = builders.collect {k, v -> [k, v]}
    for (i = 0; i < _builders.size(); i++) {
       def packageType =  _builders[i][0]
       def packageBuilder = _builders[i][1]
       if (packageBuilder) {
            builds[(packageType)] = {
                //stage("Packaging ${packageType}s") {
                if (true) { // TODO blue ocean doesn't show nested stages properly https://issues.jenkins-ci.org/browse/JENKINS-38442
                    logger.info("building ${packageType}s", logPrefix)
                    volumesOrPaths[(packageType)] = packageBuilder.call(version, sourcePath)
                }
            }
       }
    }

    builds.failFast = failFast
    parallel builds

    return volumesOrPaths
}
