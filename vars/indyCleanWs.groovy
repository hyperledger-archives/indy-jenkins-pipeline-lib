import groovy.transform.Field
@Field String logPrefix = "indyCleanWs"
@Field def logger = indyLogger

def call() {
    try {
        cleanWs()
    } catch (NoSuchMethodError ex) {
        logger.warning("failed to clean the workspace, seems ws-cleanup plugin is not installed", logPrefix)
    }
}
