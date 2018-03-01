import groovy.transform.Field
@Field String logPrefix = "indyAutoMergePR"
@Field def logger = indyLogger

import org.hyperledger.pipeline.indy.Helpers
import org.hyperledger.pipeline.indy.DockerHelpers

@Field def helpers = new Helpers()
@Field def dockerHelpers = new DockerHelpers()

def call(body) {
    stage("Automerge PR") {
        def indyConfig = indyConfig()

        prDetails = [:]
        if (env.CHANGE_ID) {
            prDetails = helpers.getRepoDetails(env.CHANGE_URL)
            prDetails.number = env.CHANGE_ID
        } else if (env.ghprbPullId) {
            prDetails = helpers.getRepoDetails(env.ghprbPullLink)
            prDetails.number = env.ghprbPullId
        }

        config = [
            node: indyConfig.indyNodeLabel,
            prDetails: prDetails,
            credentialsId: indyConfig.credentials.gitHubToken,
            // TODO values from indyConfig
            approveMessage: "Approved",
            statusState: "success",
            statusUrl: "$RUN_DISPLAY_URL",
            statusDescr: "Success: This commit looks good for auto merge",
            statusContext: indyConfig.prStatusContext,
        ]

        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()

        logger.info("parameters: $config", logPrefix)

        def err = null
        if (!config.prDetails) {
            // do not fail here
            logger.info("PR details are not defined, treat as non PR", logPrefix)
            return
        } else if (!config.credentialsId) {
            err = "no credentials provided"
        }

        if (err) {
            logger.error(err, logPrefix)
            error err
        }

        try {
            indyJNode(config.node) {
                tryAutomerge(config.prDetails, config.approveMessage,
                             config.statusState, config.statusUrl,
                             config.statusDescr, config.statusContext,
                             config.credentialsId, indyConfig.dryRun)

            }
        } catch (Exception ex) {
            logger.info("$ex (automerge scope, ignored)", logPrefix)
        }
    }
}

// TODO add better support for dryRun
def tryAutomerge(prDetails, approveMessage,
                statusState, statusUrl, statusDescr, statusContext,
                credentialsId,
                dryRun=false) {

    def rsBasePath = indyConfig().rsBasePath
    ['pr-automerge.dockerfile', 'pr_review.py', 'setup_compare.py', 'automerge.py'].each {
        helpers.resourceToWs("$rsBasePath/$it", it)
    }

    def automergeEnv = dockerHelpers.build('pr-automerge', 'pr-automerge.dockerfile .')

    logger.info("approving", logPrefix)
    withCredentials([string(credentialsId: credentialsId, variable: 'token')]) {
       def command = (
            "python3 automerge.py" +
                " $prDetails.owner $prDetails.reponame $prDetails.number $token" +
                " --body \"$approveMessage\"" +
                " --status-update" +
                " --status-state \"$statusState\"" +
                " --status-url \"$statusUrl\"" +
                " --status-descr \"$statusDescr\"" +
                " --status-context \"$statusContext\"" +
                " --verbose"
        )

        automergeEnv.inside {
            if (dryRun) {
                logger.debug("dryRun, command: $command", logPrefix)
            } else {
                sh command
            }
        }
    }
}
