pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend Build') {
            steps {
                dir('backend') {
                    bat 'mvn clean compile'
                }
            }
        }

        stage('Backend Test') {
            steps {
                dir('backend') {
                    bat 'mvn test'
                }
            }
        }

        stage('Frontend Test') {
            steps {
                dir('frontend') {
                    bat 'npm run test:run'
                }
            }
        }

        stage('Frontend Build') {
            steps {
                dir('frontend') {
                    bat 'npm run build'
                }
            }
        }

        stage('Docker Build') {
            steps {
                bat 'docker compose build'
            }
        }
    }

    post {
        success {
            echo 'CI PIPELINE PASSED'
        }

        failure {
            echo 'CI PIPELINE FAILED'
        }

        always {
            echo 'CI pipeline execution completed.'
        }
    }
}