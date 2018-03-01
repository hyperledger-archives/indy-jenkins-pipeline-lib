import groovy.transform.Field
@Field String logPrefix = "indyConfig"
@Field public static Map config = [:]
@Field def logger = indyLogger

def call(verify=true) {
    if (!config) {
        def logLevel = env.indyLogLevel ?: 'DEBUG'
        logger.setLogLevel(logLevel)

        def _stages = [:]
        def initStages = null
        try {
            initStages = indyStagesInit
        } catch (MissingPropertyException ex) {
            logger.info("indyStagesInit is not defined, use default stages", logPrefix)
        }

        if (initStages) {
            _stages = initStages()
            logger.info("Init stages result: $_stages", logPrefix)
        }

        // TODO can we reference them using string variable as e.g. methods
        // of some global scope object
        def stages = [
            ['indyIsTested', _stages.indyIsTested ?: indyIsTested],
            ['indyVerify', _stages.indyVerify ?: indyVerify],
            ['indyVerifyStatic', _stages.indyVerifyStatic ?: indyVerifyStatic],
            ['indyAutoMergePR', _stages.indyAutoMergePR ?: indyAutoMergePR],
            ['indyPackaging', _stages.indyPackaging ?: indyPackaging],
            ['indyPublish', _stages.indyPublish ?: indyPublish],
        ]

        config.putAll([
            rsBasePath: 'org/hyperledger/pipeline/indy',

            stages: stages.collectEntries {[it[0],
                [active: env."${it[0]}" == null ? true : env."${it[0]}" == "true", ref: it[1]]]},

            failFast: (env.indyFailFast == null ? false : env.indyFailFast == "true"),
            dryRun: (env.indyDryRun == null ? false : env.indyDryRun == "true"),
            logLevel: logLevel,

            // node label for most indy stages that need linux docker containers
            indyNodeLabel: env.indyNodeLabel,
            credentials: [
                 // GitHub token with rights to add comments
                 // and set statuses for PRs and merge them
                gitHubToken:  env.indyGitHubTokenCredId,
            ],

            prStatusContext: env.indyPRStatusContext,
        ])

        logger.info("Indy config: $config", logPrefix)
    }

    if (verify) {
        ; // nothing to check for now
    }

    return config
}
