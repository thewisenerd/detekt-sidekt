#!/usr/bin/groovy
pipeline {
    agent any
    options {
        timeout(time: 25, unit: 'MINUTES')
        timestamps()
    }
    environment {
        MAVEN_OPTS = "-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=WARN -Dorg.slf4j.simpleLogger.showDateTime=true -Djava.awt.headless=true -DinstallAtEnd=true -DdeployAtEnd=true"
        MAVEN_CLI_OPTS = "-U -B -e -fae -V"
    }
    stages {
        stage('Test') {
            steps {
                echo "Building and running tests"
                sh "mvn -P '!default,repo-proxy' ${MAVEN_CLI_OPTS} clean test"
            }
        }
        stage('Deploy-Jar') {
            when {
                branch 'master'
            }
            steps {
                sh "mvn -P '!default,repo-proxy' ${MAVEN_CLI_OPTS} deploy -DskipTests"
            }
        }
    }
    post {
        success {
            slackSend color: 'good', message: "<${env.BUILD_URL}|${currentBuild.fullDisplayName}> ${currentBuild.currentResult}"
        }
        failure {
            slackSend color: 'danger', message: "<${env.BUILD_URL}|${currentBuild.fullDisplayName}> ${currentBuild.currentResult}"
        }
        unstable {
            slackSend color: 'warning', message: "<${env.BUILD_URL}|${currentBuild.fullDisplayName}> ${currentBuild.currentResult}"
        }
    }
}
