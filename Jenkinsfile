#!/usr/bin/env groovy

pipeline {
    stages {
        stage('Build') {
            steps {
                echo 'Building...'
                ./gradlew assemble
            }
        }
        stage('Test') {
            steps {
                echo 'Testing...'
                ./gradlew build
            }
        }
    }
}
