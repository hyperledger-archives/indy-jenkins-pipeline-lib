import groovy.transform.Field
@Field String logPrefix = "indyNotify"
@Field def logger = indyLogger

def call(body, stageName='Notify') {
    stage(stageName) {
        def indyConfig = indyConfig()

        config = [
            email: null,
            slack: null
        ]

        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()
        logger.info("parameters: $config", logPrefix)

        if (config.email != null) {
            email(config.email, indyConfig.dryRun)
        }

        if (config.slack != null) {
            slack(config.slack, indyConfig.dryRun)
        }
    }
}

def email(opts=[:], dryRun=false) {
    def msg = [
        body: '$DEFAULT_CONTENT',
        replyTo: '$DEFAULT_REPLYTO',
        subject: '$DEFAULT_SUBJECT',
        to: '$DEFAULT_RECIPIENTS'
    ]
    msg.putAll(opts)

    logger.debug("sending email with the following parameters: $msg", logPrefix)

    if (!dryRun) {
        emailext msg
    }
}

def slack(opts=[:], dryRun=false) {
    // TODO JOB_NAME vs PROJECT_NAME
    def msg = [:]
    msg.putAll(opts)

    logger.debug("sending slack message with the following parameters: $msg", logPrefix)

    if (!dryRun) {
        slackSend msg
    }
}
