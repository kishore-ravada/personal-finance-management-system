pipeline {
    agent any

    stages {

        // ============================================================
        // 1. CHECKOUT
        // ============================================================
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // ============================================================
        // 2. GITLEAKS
        // ============================================================
        stage('Gitleaks Secret Scan') {
            steps {
                sh '''
                    echo "=========================================="
                    echo "GITLEAKS SECRET SCAN"
                    echo "=========================================="

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

        // ============================================================
        // 3. BACKEND TEST
        // ============================================================
        stage('Backend Test') {
            steps {
                dir('backend') {
                    sh '''
                        echo "=========================================="
                        echo "BACKEND TEST"
                        echo "=========================================="

                        rm -rf ~/.m2/repository/org/apache/maven/surefire

                        mvn clean test -U \
                            -Dmaven.wagon.http.retryHandler.count=5 \
                            -Dmaven.wagon.http.retryHandler.requestSeconds=10
                    '''
                }
            }
        }

        // ============================================================
        // 4. FRONTEND TEST & BUILD
        // ============================================================
        stage('Frontend Test & Build') {
            steps {
                dir('frontend') {
                    sh '''
                        echo "=========================================="
                        echo "FRONTEND TEST & BUILD"
                        echo "=========================================="

                        npm install
                        npm run test:run
                        npm run build
                    '''
                }
            }
        }

        // ============================================================
        // 5. BACKEND BUILD
        // ============================================================
        stage('Backend Build') {
            steps {
                dir('backend') {
                    sh '''
                        echo "=========================================="
                        echo "BACKEND BUILD"
                        echo "=========================================="

                        mvn package -DskipTests -U \
                            -Dmaven.wagon.http.retryHandler.count=5 \
                            -Dmaven.wagon.http.retryHandler.requestSeconds=10
                    '''
                }
            }
        }

        // ============================================================
        // 6. SONARQUBE
        // ============================================================
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    script {
                        def scannerHome = tool 'SonarScanner'

                        sh """
                            echo "=========================================="
                            echo "SONARQUBE ANALYSIS"
                            echo "=========================================="

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

        // ============================================================
        // 7. OWASP DEPENDENCY-CHECK
        // ============================================================
        stage('OWASP Dependency-Check') {
            steps {
                sh '''
                    echo "=========================================="
                    echo "OWASP DEPENDENCY-CHECK"
                    echo "=========================================="

                    mkdir -p dependency-check-report
                '''

                dependencyCheck(
                    odcInstallation: 'DependencyCheck',
                    nvdCredentialsId: 'nvd-api-key',
                    additionalArguments: '--scan . --format ALL --out dependency-check-report'
                )
            }
        }

        // ============================================================
        // 8. DOCKER BUILD
        // ============================================================
        stage('Docker Build') {
            steps {
                sh '''
                    echo "=========================================="
                    echo "DOCKER BUILD"
                    echo "=========================================="

                    docker build \
                        -t kittuuu/personal-finance-backend:latest \
                        ./backend

                    docker build \
                        -t kittuuu/personal-finance-frontend:latest \
                        ./frontend

                    echo ""
                    echo "Docker images:"
                    docker images | grep personal-finance
                '''
            }
        }

        // ============================================================
        // 9. TRIVY
        // ============================================================
        stage('Trivy Container Security Scan') {
            steps {
                sh '''
                    echo "=========================================="
                    echo "TRIVY CONTAINER SECURITY SCAN"
                    echo "=========================================="

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

        // ============================================================
        // 10. DOCKER HUB PUSH
        // ============================================================
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
                        echo "=========================================="
                        echo "DOCKER HUB PUSH"
                        echo "=========================================="

                        echo "$DOCKER_PASSWORD" | docker login \
                            -u "$DOCKER_USERNAME" \
                            --password-stdin

                        docker push kittuuu/personal-finance-backend:latest
                        docker push kittuuu/personal-finance-frontend:latest

                        docker logout

                        echo "Docker Hub push completed."
                    '''
                }
            }
        }

        // ============================================================
        // 11. DEPLOY APPLICATION
        // ============================================================
        stage('Deploy Application') {
            steps {
                sh '''
                    echo "=========================================="
                    echo "DEPLOY APPLICATION"
                    echo "=========================================="

                    echo "Current workspace:"
                    pwd

                    echo ""
                    echo "Checking deployment compose file:"
                    ls -la docker-compose.deploy.yml

                    echo ""
                    echo "Stopping previous deployment..."

                    docker compose \
                        -f docker-compose.deploy.yml \
                        down --remove-orphans || true

                    echo ""
                    echo "Removing old conflicting containers..."

                    docker rm -f finance-mysql finance-backend finance-frontend || true

                    echo ""
                    echo "Pulling latest Docker Hub images..."

                    docker compose \
                        -f docker-compose.deploy.yml \
                        pull

                    echo ""
                    echo "Starting deployment..."

                    docker compose \
                        -f docker-compose.deploy.yml \
                        up -d

                    echo ""
                    echo "Deployment started."
                '''
            }
        }

        // ============================================================
        // 12. DEPLOYMENT VERIFICATION
        // ============================================================
        stage('Deployment Verification') {
            steps {
                sh '''
                    echo "=========================================="
                    echo "DEPLOYMENT VERIFICATION"
                    echo "=========================================="

                    echo "Waiting for containers..."
                    sleep 20

                    echo ""
                    echo "========== COMPOSE STATUS =========="

                    docker compose \
                        -f docker-compose.deploy.yml \
                        ps

                    echo ""
                    echo "========== RUNNING CONTAINERS =========="

                    docker ps

                    // MYSQL
                    echo ""
                    echo "========== MYSQL HEALTH =========="

                    MYSQL_CONTAINER=$(docker compose \
                        -f docker-compose.deploy.yml \
                        ps -q mysql)

                    if [ -z "$MYSQL_CONTAINER" ]; then
                        echo "ERROR: MySQL container not found."
                        exit 1
                    fi

                    MYSQL_STATUS=$(docker inspect \
                        -f '{{.State.Health.Status}}' \
                        "$MYSQL_CONTAINER")

                    echo "MySQL status: $MYSQL_STATUS"

                    if [ "$MYSQL_STATUS" != "healthy" ]; then
                        echo "ERROR: MySQL is not healthy."
                        docker logs "$MYSQL_CONTAINER" --tail 100
                        exit 1
                    fi

                    // BACKEND
                    echo ""
                    echo "========== BACKEND HEALTH =========="

                    BACKEND_CONTAINER=$(docker compose \
                        -f docker-compose.deploy.yml \
                        ps -q backend)

                    if [ -z "$BACKEND_CONTAINER" ]; then
                        echo "ERROR: Backend container not found."
                        exit 1
                    fi

                    BACKEND_STATUS=$(docker inspect \
                        -f '{{.State.Status}}' \
                        "$BACKEND_CONTAINER")

                    echo "Backend status: $BACKEND_STATUS"

                    if [ "$BACKEND_STATUS" != "running" ]; then
                        echo "ERROR: Backend is not running."
                        docker logs "$BACKEND_CONTAINER" --tail 100
                        exit 1
                    fi

                    // FRONTEND
                    echo ""
                    echo "========== FRONTEND HEALTH =========="

                    FRONTEND_CONTAINER=$(docker compose \
                        -f docker-compose.deploy.yml \
                        ps -q frontend)

                    if [ -z "$FRONTEND_CONTAINER" ]; then
                        echo "ERROR: Frontend container not found."
                        exit 1
                    fi

                    FRONTEND_STATUS=$(docker inspect \
                        -f '{{.State.Status}}' \
                        "$FRONTEND_CONTAINER")

                    echo "Frontend status: $FRONTEND_STATUS"

                    if [ "$FRONTEND_STATUS" != "running" ]; then
                        echo "ERROR: Frontend is not running."
                        docker logs "$FRONTEND_CONTAINER" --tail 100
                        exit 1
                    fi

                    echo ""
                    echo "=========================================="
                    echo "DEPLOYMENT VERIFICATION SUCCESSFUL"
                    echo "=========================================="
                '''
            }
        }

        // ============================================================
        // 13. OWASP ZAP DAST
        // ============================================================
        stage('OWASP ZAP DAST') {
            steps {
                sh '''
                    echo "=========================================="
                    echo "OWASP ZAP DAST"
                    echo "=========================================="

                    echo "Cleaning previous ZAP report..."

                    rm -rf "$WORKSPACE/zap-report"
                    mkdir -p "$WORKSPACE/zap-report"

                    echo ""
                    echo "Report directory:"
                    ls -ld "$WORKSPACE/zap-report"

                    echo ""
                    echo "Starting ZAP baseline scan..."

                    docker run --rm \
                        --network personal-finance-management-system_finance-net \
                        -v "$WORKSPACE/zap-report:/zap/wrk:rw" \
                        ghcr.io/zaproxy/zaproxy:stable \
                        zap-baseline.py \
                        -t http://frontend \
                        -r zap-report.html \
                        -I || true

                    echo ""
                    echo "ZAP report directory contents:"
                    ls -lah "$WORKSPACE/zap-report" || true

                    if [ ! -f "$WORKSPACE/zap-report/zap-report.html" ]; then
                        echo ""
                        echo "ERROR: ZAP report was not generated."
                        exit 1
                    fi

                    echo ""
                    echo "=========================================="
                    echo "ZAP REPORT GENERATED SUCCESSFULLY"
                    echo "=========================================="

                    ls -lh "$WORKSPACE/zap-report/zap-report.html"
                '''
            }
        }
    }

    // ================================================================
    // POST ACTIONS
    // ================================================================
    post {
        always {
            echo "=========================================="
            echo "ARCHIVING SECURITY REPORTS"
            echo "=========================================="

            archiveArtifacts(
                artifacts: 'zap-report/zap-report.html',
                allowEmptyArchive: true
            )

            archiveArtifacts(
                artifacts: 'dependency-check-report/**/*',
                allowEmptyArchive: true
            )
        }

        success {
            echo '''
            ==================================================
            CI/CD PIPELINE COMPLETED SUCCESSFULLY
            ==================================================
            '''
        }

        failure {
            echo '''
            ==================================================
            CI/CD PIPELINE FAILED
            ==================================================
            Check the failed Jenkins stage and console log.
            ==================================================
            '''
        }
    }
}