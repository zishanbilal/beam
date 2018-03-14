#!/usr/bin/env groovy

node {
    stage('Build') {
        steps {
            print 'Building...'
            ./gradlew assemble
        }
    }
    stage('Test') {
        steps {
            print 'Testing...'
            ./gradlew build
        }
    }
}
