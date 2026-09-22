pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend Test') {
            agent {
                docker { image 'maven:3.9.6-eclipse-temurin-21' } // Pulls a container with Maven and Java 21 pre-installed
            }
            steps {
                dir('backend') {
                    sh 'mvn test'
                }
            }
        }

        stage('Frontend Test & Build') {
            agent {
                docker { image 'node:20-alpine' } // Pulls a container with Node.js and npm pre-installed
            }
            steps {
                dir('frontend') {
                    sh 'npm install'
                    sh 'npm run test:run'
                    sh 'npm run build'
                }
            }
        }

        stage('Backend Build') {
            agent {
                docker { image 'maven:3.9.6-eclipse-temurin-21' }
            }
            steps {
                dir('backend') {
                    sh 'mvn package -DskipTests'
                }
            }
        }

        stage('Docker Build') {
            steps {
                // Runs back on the base agent where we shared the Docker daemon socket
                sh 'docker build -t finance-backend:ci ./backend'
                sh 'docker build -t finance-frontend:ci ./frontend'
            }
        }
    }

    post {
        success {
            echo 'CI pipeline completed successfully!'
        }
        failure {
            echo 'CI pipeline failed. Check the stage logs.'
        }
    }
}
