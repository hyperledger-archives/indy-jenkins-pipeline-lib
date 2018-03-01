#!/usr/bin/groovy
package org.hyperledger.pipeline.indy

import com.cloudbees.groovy.cps.NonCPS

@NonCPS
def getRepoDetails(githubUrl) {
    def pattern = /github.com[\/:]([^\/]+)\/(.+?)(?:\/|\.git|$)/
    def matcher = (githubUrl =~ pattern)
    return !!matcher ? ["owner": matcher[0][1], "repo": matcher[0][2]] : null
}

def resourceToWs(resourceName, wsFile=null, mode='777') {
	wsFile = wsFile ?: resourceName
	def preparePackage = libraryResource resourceName
    writeFile file: wsFile, text: preparePackage
    sh "chmod $mode $wsFile"
}

return this
