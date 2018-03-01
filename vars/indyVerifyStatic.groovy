import groovy.transform.Field
@Field String logPrefix = "indyVerifyStatic"
@Field def logger = indyLogger

def call(body) {
    stage('Static code validation') {
        def indyConfig = indyConfig()
        def indyState = indyState()

        if (indyState.isTested) {
            logger.info('Skip testing because already done', logPrefix)
            return
        }

        def config = [
            node: indyConfig.indyNodeLabel,
            dockerEnv: [
                imgName: "code-validation",
                dockerfile: "ci/code-validation.dockerfile",
                contextDir: "ci"
            ]
        ]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()

        indyJNode(config.node) {
            logger.info("Checkout SCM", logPrefix)
            checkout scm

            buildDocker(config.dockerEnv.imgName,
                        config.dockerEnv.dockerfile,
                        config.dockerEnv.contextDir).inside {
                sh "python3 -m flake8"
            }
        }
    }
}


//TODO use helper class
def buildDocker(imageName, dockerfile, contextDir) {
    def uid = sh(returnStdout: true, script: 'id -u').trim()
    return docker.build("$imageName", "--build-arg uid=$uid -f $dockerfile $contextDir")
}
