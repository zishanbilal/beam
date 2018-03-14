#!/usr/bin/env groovy

node {
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
