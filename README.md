[//]: # "table of contents update: run the following from inside root folder of the repo"
[//]: # "docker run --rm -it -v `pwd`:/usr/src jorgeandrada/doctoc"

# Indy Jenkins Pipeline Library

The repository contains a library of reusable [Jenkins Pipeline](https://jenkins.io/doc/book/pipeline/) steps and functions for that are used in Hyperledger Indy projects' CI/CD pipelines.

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Indy Pipeline Steps](#indy-pipeline-steps)
  - [indyAutoMergePR](#indyautomergepr)
  - [indyCleanWs](#indycleanws)
  - [indyConfig](#indyconfig)
  - [indyJNode](#indyjnode)
  - [indyLogger](#indylogger)
    - [email](#email)
    - [slack](#slack)
  - [indyPackaging](#indypackaging)
  - [indyPipeline](#indypipeline)
  - [indyPublish](#indypublish)
  - [indyVerify](#indyverify)
  - [indyVerifyStatic](#indyverifystatic)
  - [indyStagesInit](#indystagesinit)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Indy Pipeline Steps

### indyAutoMergePR

- merges PR if changes consists only of updates for dependencies versions
- makes sense for PR jobs only

```groovy
    indyAutoMergePR {
        node = 'node-label-with-linux-docker'
                // default: indyConfig.indyNodeLabel
        prDetails = [
            owner: 'github-repo-owner',
                // default: parsed from env.CHANGE_URL or env.ghprbPullLink
            repo: 'github-repo-name',
                // default: parsed from env.CHANGE_URL or env.ghprbPullLink
            number: 1, // PR number
                // default: env.CHANGE_ID or env.ghprbPullId
        ]
        credentialsId = 'GitHub-token-with-PRs-write-permissions'
                // default: indyConfig.credentials.gitHubToken
        approveMessage = 'comment-in-PR'
                // default: 'Approved'
        statusState = 'pending|success|error|failure'
                // default: 'success'
        statusUrl = 'status-url'
                // default: env.RUN_DISPLAY_URL
        statusDescr = 'status-description'
                // default: 'Success: This commit looks good for auto merge'
        statusContext = 'status-context'
                // default: indyConfig.prStatusContext
    }
```
### indyCleanWs

- wrapper for *cleanWS* step
- doesn't fail if [ws-clenup](https://plugins.jenkins.io/ws-cleanup) plugin is not installed

```groovy
    indyCleanWs()
```
### indyConfig

- sets indy pipelines environment
- most of fields could be conigured by environment variables
- returns a Map with the following fields
  - *rsBasePath*: classpath to resources, **org/hyperledger/pipeline/indy**, non-configurable
  - *steps*: a list of Maps, where key is name of one of indy steps and values are:
    - *active*: is step active of not, depends on *env.$stepName* (default: `true`)
    - *ref*: groovy closure reference to the step, depends on [indyStagesInit](#indystagesinit) (default: indy step closure)
  - *failFast*: turns on/off failFast mode for *parallel* steps, depends on **env.indyFailFast** (default: `false`)
  - *dryRun*: turns on/off dry run mode, depends on **env.indyDryRun** (default: `false`)
  - *logLevel*: sets logging level, depends on **env.indyLogLevel** (default: `'DEBUG`')
  - *indyNodeLabel*: node label where linux docker container could be run, depends on **env.indyNodeLabel** (default: `null`)
  - *credentials*: 
    - *gitHubToken*: GitHub token with rights to add comments and set statuses for PRs and merge them, depends on **env.indyGitHubTokenCredId** (default: `null`)
  - *prStatusContext*: PR status set for the PR by indyAutoMergePR, depends on **env.indyPRStatusContext** (default: `null`)
### indyIsTested

- check if current commit has been already tested in scope of any PR
- makes sense for branch jobs only

```groovy
    indyIsTested {
        node = 'node-label-with-linux-docker'
                // default: indyConfig.indyNodeLabel
        branch = 'PRs-target-branch-filter'
                // default: env.$BRANCH_NAME
        contexts = ['PRs-context-filters', ...] //default: ['continuous-integration/jenkins/pr-merge']
        age = 5      // filter PRs by number of days from the last update, default: 7
                // default: 7
    }
```
### indyJNode

- wrapper for *node* step
- guarantees that [indyCleanWs](#indycleanws) will be called after *node* is finished

```groovy
    indyJNode(nodeLabel)
```
### indyLogger

- defines a set of log levels and provides API for each
- log levels: *TRACE*, *DEBUG*, *INFO*, *WARNING*, *ERROR*
- methods:
  - *trace(message, prefix=null)*
  - *debug(message, prefix=null)*
  - *info(message, prefix=null)*
  - *warning(message, prefix=null)*
  - *error(message, prefix=null)*
### indyNotify

- provides API to send notifications

#### email

```groovy
    indyNotify.email message
```
Please check [docs](https://plugins.jenkins.io/email-ext) for details about message attributes.

Default message:
```groovy
    [
        body: '$DEFAULT_CONTENT',
        replyTo: '$DEFAULT_REPLYTO',
        subject: '$DEFAULT_SUBJECT',
        to: '$DEFAULT_RECIPIENTS'
    ]
```
#### slack
```groovy
      indyNotify.slack message
```
Please check [docs](https://plugins.jenkins.io/slack) for details about message attributes.
### indyPackaging

- performs packaging calling package builders
- each builder should return *volumeOrPath* - docker volume name or host system absolute path to the directory where package(s) is(are) placed

```groovy
    indyPackaging {
        node = 'node-label-with-linux-docker'
                // default: indyConfig.indyNodeLabel
        builders = [deb: debBuilderClosure]
                // default: not-defined
        version = '1.2.3'
                // default: not-defined
    }
```
### indyPipeline

- calls steps in order defined via parameters
- optionally limit execution time
- optionally call failure and/or success closure callbacks

```groovy
    indyPipeline {
        :
        timeout = 90 // minutes
                // default: not defined
        onFail = onFailCallback
                // default: not defined
        onSuccess = onSuccessCallback
                // default: not defined
        // format = stageName, stageConfigClosure],
        // config closures are empty by default
        stages = [
            ['indyVerifyStatic', {}],
            ['indyVerify', {
                ...
            }],
            ['indyAutoMergePR', {}],
            ['indyPackaging', {
                version = "1.2.3"
                builders.deb = buildDebUbuntu
            }]
        ]
                // default: all steps from indyConfig with dummy closures {}
    }
```
### indyPublish

- dummy step, just a definition of API
- expected that it is implemented in another shared library
- please, use [indyStagesInit](#indystagesinit) to pass the reference to the actual implementation

```groovy
    indyPublish {
        packageName = 'myPackage'
            // default: not defined
        releaseVersion = '1.2.3'
            // default: not defined
        projectName = 'myProject'
            // default: not defined
        builders = [deb: debBuilderClosure]
            // default: not defined
    }
```
### indyVerify

- runs unit/integration tests

```groovy
    indyVerify {
        labels = ['linux', 'windows']
                // default: not defined
        tests = [
            common: [
                resFile: { "test-result-filename.${NODE_NAME}.xml" },
                    // default: 'test-result.txt'
                testDir: 'indy_common',
                    // default: '.'
                python: 'python-executable-to-use',
                    // default: 'python'
                useRunner: true,    // run or not the custom runner for tests
                    // default: false
                docker: 'ubuntu' // reference to item in 'dockers' 
                    // default: not defined
            ],
            // ...
        ]
                // default: not defined
        dockers = [
            ubuntu: [
                imgName: 'ci-image-name',
                    // default: not defined
                dockerfile: "path-to-dockerfile",
                    // default: not defined
                contextDir: "path-to-docker-build-context-dir"
                    // default: not defined
            ]
        ]
                // default: not defined

    }
```
### indyVerifyStatic

- runs flake8 based static code verification

```groovy
    indyVerifyStatic {
        node = 'node-label-with-linux-docker'
                // default: indyConfig.indyNodeLabel
        dockerEnv = [
            imgName: 'ci-image-name',
                // default: 'code-validation'
            dockerfile: "path-to-dockerfile",
                // default: 'ci/code-validation.dockerfile'
            contextDir: "path-to-docker-build-context-dir"
                // default: 'ci'
        ]
    }
```
### indyStagesInit

- **virtual step**: could be used by other shared libraries to set (replace) references for standard steps
- should return a Map:
  - key - indy step
  - value - step closure reference

e.g.
```groovy
    def call() {
        return [
            indyPublish: myLibraryPublishStep
        ]
    }
```
