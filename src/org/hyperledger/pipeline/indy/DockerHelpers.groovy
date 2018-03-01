#!/usr/bin/groovy
package org.hyperledger.pipeline.indy

def build(name, file='ci/ubuntu.dockerfile ci', customParams='') {
  def uid = sh(returnStdout: true, script: 'id -u').trim()
  return docker.build("$name-test", "--build-arg uid=${uid} ${customParams} -f $file")
}

def buildAndRunWindows(name, execs, file='ci/windows.dockerfile ci') {
  def containerName = "${name}_test_container"
  def imageTag = "${name}-windows-test"
  def error = null

  try {
    sh "docker build -t \"$imageTag\" -f $file"
    sh "docker rm --force $containerName || true"
    sh 'chmod -R a+w $PWD'
    sh "docker run -id --name $containerName -v \"\$(cygpath -w \$PWD):C:\\testOrig\" \"$imageTag\""
    // XXX robocopy will return 1, and this is OK and means success (One of more files were copied successfully),
    // that's why " || true"
    sh "docker exec -i $containerName cmd /c \"robocopy C:\\testOrig C:\\test /COPYALL /E\" || true"

    for (def item : execs) {
      sh "docker exec -i $containerName cmd /c \"${item}\""
    }
  } 
  catch(e) {
    error = e
  } 
  finally {
    echo "Stopping container $containerName"
    sh "docker stop -t 10 $containerName || true"
    sh "docker rm --force $containerName || true"
    if (error) {
      throw error
    }
  }
}

return this
