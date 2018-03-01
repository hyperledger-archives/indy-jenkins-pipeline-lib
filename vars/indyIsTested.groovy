import groovy.transform.Field
@Field String logPrefix = "indyIsTested"
@Field def logger = indyLogger

import org.hyperledger.pipeline.indy.Helpers
import org.hyperledger.pipeline.indy.DockerHelpers

@Field def helpers = new Helpers()
@Field def dockerHelpers = new DockerHelpers()


def call(body) {
   stage("Is Tested") {
        def indyConfig = indyConfig()

        def config = [
            node: indyConfig.indyNodeLabel,
            branch: env.$BRANCH_NAME,
            contexts: ["continuous-integration/jenkins/pr-merge"], //TODO env variable
            age: 7  // days, estimated from the last update
        ]

        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()

        logger.info("parameters: $config", logPrefix)

        def indyState = indyState()
        def res = indyState.isTested

        try {
            indyJNode(config.node) {
                res = check(config.branch, config.contexts, config.age, indyConfig.dryRun)
            }
        } catch (Exception ex) {
            logger.info("$ex (isTested scope, ignored)", logPrefix)
        }

        indyState.isTested = res
        return res
    }
}


def check(targetBranch, contexts, age, dryRun=false) {
    def found = false

    logger.info("Checkout csm", logPrefix)
    checkout scm

    logger.info("prepare tools and env", logPrefix)

    def sha = sh(returnStdout: true, script: "git rev-parse HEAD^{commit}").trim()
    def gitOriginUrl = sh(returnStdout: true, script: "git config --get remote.origin.url").trim()
    def repoDetails = helpers.getRepoDetails(gitOriginUrl)

    def _branch = targetBranch ? "--branch $targetBranch" : ""

    def _contexts = ""
    for (int i = 0; i < contexts.size(); i++) {
        _contexts += " --context " + contexts[i]
    }

    def rsBasePath = indyConfig().rsBasePath
    helpers.resourceToWs("$rsBasePath/match-passed-PR.py")
    helpers.resourceToWs("$rsBasePath/pr-automerge.dockerfile")
    def dockerEnv = dockerHelpers.build('match-passed-pr', "$rsBasePath/pr-automerge.dockerfile .")

    logger.info("matching...", logPrefix)
    withCredentials([string(credentialsId: 'evernym-github-machine-user-token', variable: 'token')]) {
        def command = (
            "python3 $rsBasePath/match-passed-PR.py $repoDetails.owner" +
            " $repoDetails.repo $sha $token $_branch $_contexts --updated $age --verbose"
        )

        dockerEnv.inside {
            if (dryRun) {
                logger.debug("dryRun, command: $command", logPrefix)
            } else {
                def prUrl = sh(returnStdout: true, script: command).trim()

                if (prUrl) {
                    logger.info("found matched PR: $prUrl", logPrefix)
                } else {
                    logger.info("no matched PR found", logPrefix)
                }

                found = !!prUrl
            }
        }
    }

    return found
}
