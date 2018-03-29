pipeline {
  agent any
  stages {
    stage('checkout') {
      steps {
        git(url: 'https://github.com/zishanbilal/beam.git', branch: '*/master', changelog: true, poll: true)
      }
    }
    stage('build') {
      steps {
        sh './gradlew build'
      }
    }
  }
}