pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Gitleaks Secret Scan') {
              steps {
                 sh '''
                  docker run --rm \
                  -v "$WORKSPACE:/repo" \
                  zricethezav/gitleaks:latest \
                  detect --source=/repo --no-banner --redact
                  '''
                }
             }

        stage('Backend Test') {
            steps {
                dir('backend') {
                    sh 'mvn test'
                }
            }
        }

        stage('Frontend Test & Build') {
            steps {
                dir('frontend') {
                    sh 'npm install'
                    sh 'npm run test:run'
                    sh 'npm run build'
                }
            }
        }

        stage('Backend Build') {
            steps {
                dir('backend') {
                    sh 'mvn package -DskipTests'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    script {

                        def scannerHome = tool 'SonarScanner'

                        sh """
                            ${scannerHome}/bin/sonar-scanner \
                              -Dsonar.projectKey=personal-finance-management \
                              -Dsonar.projectName="Personal Finance Management" \
                              -Dsonar.sources=backend/src/main,frontend/src \
                              -Dsonar.java.binaries=backend/target/classes \
                              -Dsonar.exclusions=**/node_modules/**,**/target/**,**/dist/**
                        """
                    }
                }
            }
        }

        stage('OWASP Dependency-Check') {
            steps {

                sh 'mkdir -p dependency-check-report'

                dependencyCheck(
                    odcInstallation: 'DependencyCheck',
                    nvdCredentialsId: 'nvd-api-key',
                    additionalArguments: '--scan . --format ALL --out dependency-check-report'
                )
            }
        }

        stage('Docker Build') {
            steps {

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