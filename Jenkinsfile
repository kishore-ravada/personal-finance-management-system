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
                sh '''
                    docker build \
                        -t kittuuu/personal-finance-backend:latest \
                        ./backend

                    docker build \
                        -t kittuuu/personal-finance-frontend:latest \
                        ./frontend
                '''
            }
        }

        stage('Trivy Container Security Scan') {
            steps {
                sh '''
                    echo "Running lightweight filesystem security scan..."
                    docker run --rm \
                        -v "$WORKSPACE:/workspace" \
                        aquasec/trivy:latest \
                        fs \
                        --scanners vuln \
                        --severity HIGH,CRITICAL \
                        --exit-code 1 \
                        /workspace
                '''
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {
                    sh '''
                        echo "$DOCKER_PASSWORD" | docker login \
                            -u "$DOCKER_USERNAME" \
                            --password-stdin

                        docker push kittuuu/personal-finance-backend:latest
                        docker push kittuuu/personal-finance-frontend:latest

                        docker logout
                    '''
                }
            }
        }

        stage('Deploy Application') {
            steps {
                sh '''
                    echo "Force cleaning any lingering conflicting containers..."
                    docker rm -f finance-mysql finance-backend finance-frontend || true

                    echo "Stopping and cleaning previous deployment..."
                    docker compose \
                        -f docker-compose.deploy.yml \
                        down --remove-orphans || true

                    echo "Pulling latest images..."
                    docker compose \
                        -f docker-compose.deploy.yml \
                        pull

                    echo "Starting deployment..."
                    docker compose \
                        -f docker-compose.deploy.yml \
                        up -d
                '''
            }
        }

        stage('Deployment Verification') {
            steps {
                sh '''
                    echo "Waiting for containers to initialize..."
                    sleep 20

                    echo "========== CONTAINER STATUS =========="
                    docker compose \
                        -f docker-compose.deploy.yml \
                        ps

                    echo "========== MYSQL HEALTH =========="
                    MYSQL_STATUS=$(docker inspect \
                        -f '{{.State.Health.Status}}' \
                        personal-finance-management-system-mysql-1)

                    echo "MySQL status: $MYSQL_STATUS"

                    if [ "$MYSQL_STATUS" != "healthy" ]; then
                        echo "ERROR: MySQL is not healthy."
                        docker logs personal-finance-management-system-mysql-1 --tail 100
                        exit 1
                    fi

                    echo "======================================"
                    echo "DEPLOYMENT VERIFICATION SUCCESSFUL"
                    echo "======================================"
                '''
            }
        }
     } 

     stage('OWASP ZAP DAST') {
       steps {
           sh '''
              mkdir -p zap-report

                docker run --rm \
                --network personal-finance-management-system_finance-net \
                -v "$WORKSPACE/zap-report:/zap/wrk/:rw" \
                ghcr.io/zaproxy/zaproxy:stable \
                zap-baseline.py \
                -t http://frontend \
                -r zap-report.html
        '''
    }
}

    post {
        success {
            echo '''
            CI/CD pipeline completed successfully!

            Security:
            - Gitleaks
            - SonarQube
            - OWASP Dependency-Check
            - Trivy

            Deployment:
            - Docker Hub Push
            - Docker Compose Deployment
            - Deployment Verification
            '''
        }

        failure {
            echo '''
            CI/CD pipeline failed.

            Check the failed stage and Jenkins console output.
            Possible causes:
            - Tests failed
            - Security scan failed
            - Docker build failed
            - Docker Hub authentication failed
            - Docker push failed
            - Deployment failed
            - Container health check failed
            '''
        }
    }
}