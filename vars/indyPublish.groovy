import groovy.transform.Field
@Field String logPrefix = "indyPublish"
@Field def logger = indyLogger

def call(body) {
    config = [
        packageName: null,      // package name
        releaseVersion: null,   // release name
        projectName: null,      // project name ???
        builders: [:],          // format: [builderType(packageType): builerClosure]
    ]

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    logger.info("parameters: $config", logPrefix)

    logger.warning("Not implemented", logPrefix)
}
