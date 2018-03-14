#!/usr/bin/env groovy

node {
    git 'https://github.com/zishanbilal/beam.git'

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
