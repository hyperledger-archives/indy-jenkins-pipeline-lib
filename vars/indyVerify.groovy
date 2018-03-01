import groovy.transform.Field
@Field String logPrefix = "indyVerify"
@Field def logger = indyLogger

def call(body) {
    stage('Build / Test') {
        def indyState = indyState()

        if (indyState.isTested) {
            logger.info('Skip testing because already done', logPrefix)
            return
        }

        def indyConfig = indyConfig()

        def config = [
            // list of nodes labels where perform each test
            labels: [],
            // Map of tests, format [testName: testParams],
            // TODO move all specific things to jenkinsfiles
            tests: [],
            // Map of docker parameters for node labels,
            // format: [node-label: [imgName, dockerfile, contextDir]]
            dockers: [],
        ]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()

        logger.info("parameters: $config", logPrefix)

        def builds = [:]
        config.labels.each { label ->
            def descr = "${label}Test"

            config.tests.each { testName, testParams ->
                def dockerEnv = config.dockers[testParams.docker]
                def currDescr = "${descr}-${testName}"

                builds[(currDescr)] = {
                    stage(currDescr) {
                        indyJNode(label) {
                            withTestEnv dockerEnv.imgName, dockerEnv.dockerfile, dockerEnv.contextDir, { python ->
                                logger.info("Test ${currDescr}", logPrefix)
                                testParams.python = python
                                test(testParams)
                            }
                        }
                    }
                }
            }
        }

        builds.failFast = indyConfig.failFast

        parallel builds
    }
}

def buildDocker(imageName, dockerfile, contextDir) {
    def uid = sh(returnStdout: true, script: 'id -u').trim()
    return docker.build("$imageName", "--build-arg uid=$uid -f $dockerfile $contextDir")
}

def install(options=[:]) {
    options.pip = options.pip ?: 'pip'
    options.isVEnv = options.isVEnv ?: false
    options.deps = options.deps ?: []

    for (def dep : options.deps) {
        sh "$options.pip install " + (options.isVEnv ? "-U" : "") + " $dep"
    }

    // TODO check that `--ignore-installed` case works when windows is enabled
    // (makes sense only for virtual envs with `--system-site-packages`)
    sh "$options.pip install " + (options.isVEnv ? "--ignore-installed" : "") + " .[tests]"
}


def withTestEnv(imgName, dockerfile, contextDir, body) {
    logger.info("Checkout csm", logPrefix)
    checkout scm

    if (isUnix()) {
        logger.info("Build docker image", logPrefix)

        buildDocker(imgName, dockerfile, contextDir).inside {
            logger.info("Install dependencies", logPrefix)
            install()
            body.call('python')
        }
    } else { // windows expected
        logger.info("Build virtualenv", logPrefix)
        def virtualEnvDir = ".venv"
        sh "virtualenv --system-site-packages $virtualEnvDir"

        logger.info("Install dependencies", logPrefix)
        install(pip: "$virtualEnvDir/Scripts/pip", isVenv: true)
        body.call("$virtualEnvDir/Scripts/python")
    }
}


def test(options=[:]) {
    // TODO other way to get actual resFile name
    // (instanceof and other type detection related things are not allowed
    // inside jenkins Groovy Sandbox)
    options.resFile = options.resFile ? options.resFile.call() : 'test-result.txt'
    options.testDir = options.testDir ?: '.'
    options.python = options.python ?: 'python'
    options.useRunner = options.useRunner ?: false
    options.testOnlySlice = options.testOnlySlice ?: '1/1'

    logger.debug("test run options: $options", logPrefix)

    try {
        if (options.useRunner) {
            sh "PYTHONASYNCIODEBUG='0' $options.python runner.py --pytest \"$options.python -m pytest\" --dir $options.testDir --output \"$options.resFile\" --test-only-slice \"$options.testOnlySlice\""
        } else {
            sh "$options.python -m pytest --junitxml=$options.resFile $options.testDir"
        }
    }
    finally {
        try {
            sh "ls -la $options.resFile"
        } catch (Exception ex) {
            // pass
        }

        if (options.useRunner) {
            archiveArtifacts allowEmptyArchive: true, artifacts: "$options.resFile"
        } else {
            junit "$options.resFile"
        }
    }
}
