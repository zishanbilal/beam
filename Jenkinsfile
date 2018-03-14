#!/usr/bin/env groovy

stages {
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
