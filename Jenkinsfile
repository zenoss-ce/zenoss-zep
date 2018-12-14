#!/usr/bin/env groovy

node {

  stage('Checkout') {
    checkout scm
  }


  stage('Build') {
    docker.image('zenoss/build-tools:0.0.10').inside() { 
      withMaven(mavenSettingsConfig: 'bintray') {
        sh '''
          export PATH=$MVN_CMD_DIR:$PATH 
          mvn -B package
        '''
      }
    }
  }

  stage('Publish app') {
    def remote = [:]
    withFolderProperties {
      withCredentials( [sshUserPrivateKey(credentialsId: 'PUBLISH_SSH_KEY', keyFileVariable: 'identity', passphraseVariable: '', usernameVariable: 'userName')] ) {
        remote.name = env.PUBLISH_SSH_HOST
        remote.host = env.PUBLISH_SSH_HOST
        remote.user = userName
        remote.identityFile = identity
        remote.allowAnyHosts = true

        def tar_ver = sh( returnStdout: true, script: "awk -F'(>|<)' '/artifact.+zep-parent/{getline; print \$3}' pom.xml" ).trim()
        sshPut remote: remote, from: 'dist/target/zep-dist-' + tar_ver + '.tar.gz', into: env.PUBLISH_SSH_DIR
      }
    }
  }
}
