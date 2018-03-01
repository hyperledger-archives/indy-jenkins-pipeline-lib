import groovy.transform.Field
@Field String logPrefix = "indyJNode"
@Field def logger = indyLogger

def call(label=null, body) {
    logger.debug("Running on `${label}` node type", logPrefix)
    node(label) {
        try {
            body()
        } finally {
            logger.info("Cleanup", logPrefix)
            indyCleanWs()
        }
    }
}
