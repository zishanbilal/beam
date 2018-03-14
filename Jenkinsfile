#!/usr/bin/env groovy

node {
    stage('Build') {
        print 'Building...'
        ./gradlew assemble
    }
    stage('Test') {
        print 'Testing...'
        ./gradlew build
    }
}
