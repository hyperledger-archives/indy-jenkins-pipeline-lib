import groovy.transform.Field
@Field String logPrefix = "indyPipeline"
@Field def logger = indyLogger

def call(body) {
    def indyConfig = indyConfig()

    def config = [
        timeout: null,      // minutes
        onFail: null,       // called on fail
        onSuccess: null,    // called on success
        // format: stageName, stageConfigClosure],
        // config closures are empty by default
        stages: indyConfig.stages.collect { k, _ -> [k, {}] }
    ]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    logger.info("parameters: $config", logPrefix)

    try {
        if (config.timeout > 0) {
            timeout(config.timeout) {
                _pipeline(config)
            }
        } else {
            _pipeline(config)
        }
        currentBuild.result = 'SUCCESS'
        if (config.onSuccess) {
            config.onSuccess.call()
        }
    } catch (Exception err) {
        logger.error(err, logPrefix)
        currentBuild.result = 'FAILURE'
        if (config.onFail) {
            config.onFail.call(err)
        }
        throw err
    }
}

def _pipeline(config) {
    def indyConfig = indyConfig()

    config.stages.each { st ->
        if (indyConfig.stages[st[0]]?.active) {
            indyConfig.stages[st[0]].ref.call(st[1])
        } else {
            logger.info("Skipping stage ${st[0]}", logPrefix)
        }
    }
}
