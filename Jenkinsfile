#!/usr/bin/env groovy

node {
    ec2 cloud: 'Jenkins Cloud', template: 'ec2'

    checkout([$class: 'GitSCM', 
              branches: [[name: '*/master']], 
              doGenerateSubmoduleConfigurations: false, 
              extensions: [[$class: 'GitLFSPull']], 
              submoduleCfg: [], 
              userRemoteConfigs: [[url: 'https://github.com/zishanbilal/beam.git']]])

    stage('Build') {
        print 'Building...'
        gradle 'assemble'
    }
    stage('Test') {
        print 'Testing...'
        gradle 'build'
    }
}

def gradle(command) {
    sh "./gradlew ${command}"
}
