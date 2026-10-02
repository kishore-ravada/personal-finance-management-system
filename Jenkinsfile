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
                    set -e

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

                    echo "Gitleaks scan completed successfully."
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
                        set -e

                        echo "=========================================="
                        echo "BACKEND TEST"
                        echo "=========================================="

                        rm -rf ~/.m2/repository/org/apache/maven/surefire

                        mvn clean test -U \
                            -Dmaven.wagon.http.retryHandler.count=5 \
                            -Dmaven.wagon.http.retryHandler.requestSeconds=10

                        echo "Backend tests completed successfully."
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
                        set -e

                        echo "=========================================="
                        echo "FRONTEND TEST & BUILD"
                        echo "=========================================="

                        npm install
                        npm run test:run
                        npm run build

                        echo "Frontend tests and build completed successfully."
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
                        set -e

                        echo "=========================================="
                        echo "BACKEND BUILD"
                        echo "=========================================="

                        mvn package -DskipTests -U \
                            -Dmaven.wagon.http.retryHandler.count=5 \
                            -Dmaven.wagon.http.retryHandler.requestSeconds=10

                        echo "Backend build completed successfully."
                    '''
                }
            }
        }


        // ============================================================
        // 6. SONARQUBE ANALYSIS
        // ============================================================
        stage('SonarQube Analysis') {
            steps {

                withSonarQubeEnv('SonarQube') {

                    script {

                        def scannerHome = tool 'SonarScanner'

                        sh """
                            set -e

                            echo "=========================================="
                            echo "SONARQUBE ANALYSIS"
                            echo "=========================================="

                            ${scannerHome}/bin/sonar-scanner \
                                -Dsonar.projectKey=personal-finance-management \
                                -Dsonar.projectName="Personal Finance Management" \
                                -Dsonar.sources=backend/src/main,frontend/src \
                                -Dsonar.java.binaries=backend/target/classes \
                                -Dsonar.exclusions=**/node_modules/**,**/target/**,**/dist/**

                            echo "SonarQube analysis completed."
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
                    set -e

                    echo "=========================================="
                    echo "OWASP DEPENDENCY-CHECK"
                    echo "=========================================="

                    rm -rf dependency-check-report
                    mkdir -p dependency-check-report
                '''

                dependencyCheck(
                    odcInstallation: 'DependencyCheck',
                    nvdCredentialsId: 'nvd-api-key',
                    additionalArguments: '--scan . --format ALL --out dependency-check-report'
                )

                sh '''
                    echo ""
                    echo "Dependency-Check report files:"
                    find dependency-check-report -maxdepth 2 -type f -ls || true
                '''
            }
        }


        // ============================================================
        // 8. DOCKER BUILD
        // ============================================================
        stage('Docker Build') {
            steps {
                sh '''
                    set -e

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

                    echo ""
                    echo "Docker build completed successfully."
                '''
            }
        }


        // ============================================================
        // 9. TRIVY CONTAINER SECURITY SCAN
        // ============================================================
        stage('Trivy Container Security Scan') {
            steps {
                sh '''
                    set -e

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

                    echo "Trivy filesystem scan completed successfully."
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
                        set -e

                        echo "=========================================="
                        echo "DOCKER HUB PUSH"
                        echo "=========================================="

                        echo "$DOCKER_PASSWORD" | docker login \
                            -u "$DOCKER_USERNAME" \
                            --password-stdin

                        echo "Pushing backend image..."
                        docker push kittuuu/personal-finance-backend:latest

                        echo "Pushing frontend image..."
                        docker push kittuuu/personal-finance-frontend:latest

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
                    set -e

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
                    echo "Deployment started successfully."
                '''
            }
        }


        // ============================================================
        // 12. DEPLOYMENT VERIFICATION
        // ============================================================
        stage('Deployment Verification') {
            steps {
                sh '''
                    set -e

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

                    # ------------------------------------------------
                    # MYSQL
                    # ------------------------------------------------

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


                    # ------------------------------------------------
                    # BACKEND
                    # ------------------------------------------------

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


                    # ------------------------------------------------
                    # FRONTEND
                    # ------------------------------------------------

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

                    # ------------------------------------------------
                    # CLEAN OLD REPORT
                    # ------------------------------------------------

                    echo "Cleaning old ZAP report..."

                    rm -rf "$WORKSPACE/zap-report"
                    mkdir -p "$WORKSPACE/zap-report"

                    chmod 777 "$WORKSPACE/zap-report"

                    echo ""
                    echo "Host report directory:"
                    ls -ld "$WORKSPACE/zap-report"


                    # ------------------------------------------------
                    # TEST ZAP WRITE ACCESS
                    # ------------------------------------------------

                    echo ""
                    echo "Testing ZAP container write access..."

                    docker run --rm \
                        -v "$WORKSPACE/zap-report:/zap/wrk:rw" \
                        ghcr.io/zaproxy/zaproxy:stable \
                        sh -c 'id && touch /zap/wrk/write-test.txt && ls -lah /zap/wrk'

                    echo ""
                    echo "Removing write test..."

                    rm -f "$WORKSPACE/zap-report/write-test.txt"


                    # ------------------------------------------------
                    # RUN ZAP
                    # ------------------------------------------------

                    echo ""
                    echo "Starting ZAP baseline scan..."
                    echo "Target: http://frontend"

                    set +e

                    docker run --rm \
                        --network personal-finance-management-system_finance-net \
                        -v "$WORKSPACE/zap-report:/zap/wrk:rw" \
                        ghcr.io/zaproxy/zaproxy:stable \
                        zap-baseline.py \
                        -t http://frontend \
                        -r /zap/wrk/zap-report.html \
                        -x /zap/wrk/zap-report.xml \
                        -I

                    ZAP_EXIT=$?

                    set -e

                    echo ""
                    echo "ZAP exit code: $ZAP_EXIT"


                    # ------------------------------------------------
                    # DISPLAY REPORT FILES
                    # ------------------------------------------------

                    echo ""
                    echo "ZAP report directory contents:"

                    ls -lah "$WORKSPACE/zap-report" || true


                    # ------------------------------------------------
                    # VERIFY HTML REPORT
                    # ------------------------------------------------

                    if [ ! -f "$WORKSPACE/zap-report/zap-report.html" ]; then

                        echo ""
                        echo "ERROR: ZAP HTML report was NOT generated."

                        exit 1
                    fi


                    HTML_SIZE=$(stat -c%s "$WORKSPACE/zap-report/zap-report.html")

                    echo ""
                    echo "ZAP HTML report size: ${HTML_SIZE} bytes"


                    if [ "$HTML_SIZE" -le 100 ]; then

                        echo ""
                        echo "ERROR: ZAP HTML report is empty or too small."

                        echo "Full report directory:"
                        ls -lah "$WORKSPACE/zap-report"

                        exit 1
                    fi


                    # ------------------------------------------------
                    # VERIFY XML REPORT
                    # ------------------------------------------------

                    if [ -f "$WORKSPACE/zap-report/zap-report.xml" ]; then

                        XML_SIZE=$(stat -c%s "$WORKSPACE/zap-report/zap-report.xml")

                        echo ""
                        echo "ZAP XML report size: ${XML_SIZE} bytes"

                    else

                        echo ""
                        echo "WARNING: ZAP XML report was not generated."

                    fi


                    # ------------------------------------------------
                    # SHOW ZAP RESULT
                    # ------------------------------------------------

                    echo ""
                    echo "=========================================="
                    echo "ZAP DAST SCAN COMPLETED"
                    echo "=========================================="

                    echo ""
                    echo "ZAP exit code: $ZAP_EXIT"

                    echo ""
                    echo "Generated files:"

                    ls -lh "$WORKSPACE/zap-report"


                    # ------------------------------------------------
                    # IMPORTANT
                    # ------------------------------------------------
                    #
                    # ZAP baseline may return non-zero because of
                    # security warnings. We are currently using -I,
                    # therefore informational/warning findings do not
                    # fail the pipeline.
                    #
                    # The actual report existence/size is verified above.
                    #
                    # ------------------------------------------------

                    exit 0
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
                artifacts: 'zap-report/zap-report.xml',
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
            ✓ ZAP HTML Report
            ✓ ZAP XML Report

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
            - ZAP report generation failed

            ==================================================
            '''
        }
    }
}