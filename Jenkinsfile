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
                        detect \
                        --source=/repo \
                        --no-git \
                        --no-banner \
                        --redact
                '''
            }
        }

        stage('Backend Test') {
            steps {
                dir('backend') {
                    // Clean up any corrupted surefire cache and run tests with network retry handlers
                    sh 'rm -rf ~/.m2/repository/org/apache/maven/surefire'
                    sh 'mvn clean test -U -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.http.retryHandler.requestSeconds=10'
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
                    sh 'mvn package -DskipTests -U -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.http.retryHandler.requestSeconds=10'
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
                sh 'docker build -t personal-finance-management-backend:latest ./backend'
                sh 'docker build -t personal-finance-management-frontend:latest ./frontend'
            }
        }

        stage('Trivy Container Security Scan') {
            steps {
                // Quality Gate: Fails pipeline if HIGH or CRITICAL vulnerabilities are found
                sh 'trivy image --severity HIGH,CRITICAL --exit-code 1 personal-finance-management-backend:latest'
                sh 'trivy image --severity HIGH,CRITICAL --exit-code 1 personal-finance-management-frontend:latest'
            }
        }
    }

    post {
        success {
            echo '🎉 CI pipeline completed successfully! All security and quality gates passed.'
        }
        failure {
            echo '❌ CI pipeline failed due to build errors, security vulnerabilities, or secrets detected!'
        }
    }
}