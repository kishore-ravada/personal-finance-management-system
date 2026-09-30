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
        // 2. GITLEAKS SECRET SCAN
        // ============================================================
        stage('Gitleaks Secret Scan') {
            steps {
                sh '''
                    echo "=========================================="
                    echo "Running Gitleaks Secret Scan"
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
                        echo "Running Backend Tests"
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
                        echo "Running Frontend Tests"
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
                        echo "Building Backend"
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
                            echo "Running SonarQube Analysis"
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
                    echo "Running OWASP Dependency-Check"
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
                    echo "Building Docker Images"
                    echo "=========================================="

                    docker build \
                        -t kittuuu/personal-finance-backend:latest \
                        ./backend

                    docker build \
                        -t kittuuu/personal-finance-frontend:latest \
                        ./frontend

                    echo "Docker images built successfully."
                    docker images | grep personal-finance
                '''
            }
        }


        // ============================================================
        // 9. TRIVY CONTAINER SECURITY SCAN
        // ============================================================
        stage('Trivy Container Security Scan') {
            steps {
                sh '''
                    echo "=========================================="
                    echo "Running Trivy Security Scan"
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
                        echo "Logging into Docker Hub"
                        echo "=========================================="

                        echo "$DOCKER_PASSWORD" | docker login \
                            -u "$DOCKER_USERNAME" \
                            --password-stdin

                        echo "Pushing backend image..."
                        docker push kittuuu/personal-finance-backend:latest

                        echo "Pushing frontend image..."
                        docker push kittuuu/personal-finance-frontend:latest

                        echo "Logging out from Docker Hub..."
                        docker logout

                        echo "Docker Hub push completed successfully."
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
                    echo "DEPLOYING APPLICATION"
                    echo "=========================================="

                    echo "Workspace:"
                    pwd

                    echo "Checking deployment compose file..."
                    ls -la docker-compose.deploy.yml

                    echo "Stopping previous deployment..."
                    docker compose \
                        -f docker-compose.deploy.yml \
                        down --remove-orphans || true

                    echo "Removing old conflicting containers..."
                    docker rm -f finance-mysql finance-backend finance-frontend || true

                    echo "Pulling latest Docker Hub images..."
                    docker compose \
                        -f docker-compose.deploy.yml \
                        pull

                    echo "Starting deployment..."
                    docker compose \
                        -f docker-compose.deploy.yml \
                        up -d

                    echo "Deployment command completed."
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

                    echo "Waiting for application containers..."
                    sleep 20

                    echo ""
                    echo "========== DOCKER COMPOSE STATUS =========="
                    docker compose \
                        -f docker-compose.deploy.yml \
                        ps

                    echo ""
                    echo "========== RUNNING CONTAINERS =========="
                    docker ps

                    echo ""
                    echo "========== MYSQL HEALTH =========="

                    MYSQL_CONTAINER=$(docker compose \
                        -f docker-compose.deploy.yml \
                        ps -q mysql)

                    if [ -z "$MYSQL_CONTAINER" ]; then
                        echo "ERROR: MySQL container was not found."
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

                    echo ""
                    echo "========== BACKEND HEALTH =========="

                    BACKEND_CONTAINER=$(docker compose \
                        -f docker-compose.deploy.yml \
                        ps -q backend)

                    if [ -z "$BACKEND_CONTAINER" ]; then
                        echo "ERROR: Backend container was not found."
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

                    echo ""
                    echo "========== FRONTEND HEALTH =========="

                    FRONTEND_CONTAINER=$(docker compose \
                        -f docker-compose.deploy.yml \
                        ps -q frontend)

                    if [ -z "$FRONTEND_CONTAINER" ]; then
                        echo "ERROR: Frontend container was not found."
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

                    echo "Creating ZAP report directory..."

                    mkdir -p "$WORKSPACE/zap-report"

                    # ZAP runs as a non-root user inside the container.
                    # Give the mounted report directory write permission.
                    chmod -R 777 "$WORKSPACE/zap-report"

                    echo "Report directory permissions:"
                    ls -ld "$WORKSPACE/zap-report"

                    echo ""
                    echo "Starting ZAP baseline scan..."
                    echo ""

                    docker run --rm \
                        --network personal-finance-management-system_finance-net \
                        -v "$WORKSPACE/zap-report:/zap/wrk:rw" \
                        ghcr.io/zaproxy/zaproxy:stable \
                        zap-baseline.py \
                        -t http://frontend \
                        -r zap-report.html \
                        -I

                    echo ""
                    echo "ZAP DAST scan completed."

                    echo ""
                    echo "Generated ZAP files:"
                    ls -lah "$WORKSPACE/zap-report"
                '''
            }
        }
    }


    // ================================================================
    // POST ACTIONS
    // ================================================================
    post {

        always {

            echo "Archiving security reports..."

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

            SHIFT-LEFT SECURITY
            -------------------
            ✓ Gitleaks
            ✓ Backend Tests
            ✓ Frontend Tests
            ✓ SonarQube
            ✓ OWASP Dependency-Check
            ✓ Docker Build
            ✓ Trivy Container Scan

            CONTAINER REGISTRY
            ------------------
            ✓ Docker Hub Push

            DEPLOYMENT
            ----------
            ✓ Docker Compose Deployment
            ✓ MySQL Health Verification
            ✓ Backend Verification
            ✓ Frontend Verification

            SHIFT-RIGHT SECURITY
            --------------------
            ✓ OWASP ZAP DAST

            ==================================================
            '''
        }


        failure {

            echo '''
            ==================================================
            CI/CD PIPELINE FAILED
            ==================================================

            Check the failed Jenkins stage and console log.

            Possible causes:

            - Gitleaks detected secrets
            - Backend tests failed
            - Frontend tests failed
            - SonarQube failed
            - Dependency-Check failed
            - Docker build failed
            - Trivy detected vulnerabilities
            - Docker Hub authentication failed
            - Docker push failed
            - Deployment failed
            - Container health verification failed
            - OWASP ZAP failed

            ==================================================
            '''
        }
    }
}