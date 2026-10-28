pipeline {
    agent any

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-21-amazon-corretto.x86_64'
        PATH = "${JAVA_HOME}/bin:${PATH}"
        SONAR_HOST_URL = 'http://35.175.197.238:9000'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Verify Tools') {
            steps {
                sh 'java -version'
                sh 'mvn -version'
                sh 'ansible --version'
                sh 'docker --version'
                sh 'aws --version'
            }
        }

        stage('Build and Test') {
            steps {
                sh 'mvn -B clean verify'
            }
        }

        stage('Verify JaCoCo Report') {
            steps {
                sh '''
                    set -eu

                    test -s target/site/jacoco/jacoco.xml
                    test -s target/site/jacoco/index.html

                    echo "JaCoCo XML and HTML reports found."
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonar-server') {
                    withCredentials([
                        string(
                            credentialsId: 'sonar-token',
                            variable: 'SONAR_TOKEN'
                        )
                    ]) {
                        sh '''
                            set +x
                            set -eu

                            mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                                -Dsonar.projectKey=DevOps-Calculator-Web-Yashwanth \
                                -Dsonar.projectName="DevOps Calculator Web - Yashwanth" \
                                -Dsonar.host.url="$SONAR_HOST_URL" \
                                -Dsonar.token="$SONAR_TOKEN" \
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                                -Dsonar.coverage.exclusions="**/CalculatorServlet.java"
                        '''
                    }
                }
            }
        }

        stage('Verify WAR') {
            steps {
                sh '''
                    set -eu

                    test -f target/calculator.war
                    ls -lh target/calculator.war
                '''
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Deploy to Nexus') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus-user',
                        usernameVariable: 'NEXUS_USERNAME',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {
                    sh '''
                        set +x
                        set -eu

                        umask 077

                        cat > "$WORKSPACE/.mvn-settings.xml" <<EOF
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0">
    <servers>
        <server>
            <id>nexus-releases</id>
            <username>${NEXUS_USERNAME}</username>
            <password>${NEXUS_PASSWORD}</password>
        </server>
    </servers>
</settings>
EOF

                        mvn -B -s "$WORKSPACE/.mvn-settings.xml" deploy

                        rm -f "$WORKSPACE/.mvn-settings.xml"
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    env.AWS_REGION = 'us-east-1'
                    env.AWS_ACCOUNT_ID = '687633314352'
                    env.ECR_REPOSITORY = 'student-webapp'

                    env.ECR_REGISTRY = "${env.AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"

                    env.IMAGE_TAG = "build-${env.BUILD_NUMBER}"

                    env.ECR_IMAGE = "${env.ECR_REGISTRY}/${env.ECR_REPOSITORY}:${env.IMAGE_TAG}"

                    echo "AWS Region: ${env.AWS_REGION}"
                    echo "ECR Registry: ${env.ECR_REGISTRY}"
                    echo "ECR Repository: ${env.ECR_REPOSITORY}"
                    echo "Docker Image: ${env.ECR_IMAGE}"
                }

                sh '''
                    set -eu

                    test -f Dockerfile
                    test -f target/calculator.war

                    ls -lh target/calculator.war

                    docker build \
                        -t "$ECR_IMAGE" \
                        .
                '''
            }
        }

        stage('Login to Amazon ECR') {
            steps {
                sh '''
                    set -eu

                    aws ecr get-login-password \
                        --region "$AWS_REGION" |
                    docker login \
                        --username AWS \
                        --password-stdin "$ECR_REGISTRY"
                '''
            }
        }

        stage('Push Docker Image to ECR') {
            steps {
                sh '''
                    set -eu

                    docker push "$ECR_IMAGE"

                    docker logout "$ECR_REGISTRY"
                '''
            }
        }

        stage('Provision Tomcat Server via Ansible') {
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'ansible-target-ssh-student',
                        keyFileVariable: 'ANSIBLE_SSH_KEY',
                        usernameVariable: 'ANSIBLE_SSH_USER'
                    )
                ]) {
                    sh '''
                        set -eu

                        export ANSIBLE_CONFIG="$WORKSPACE/ansible/ansible.cfg"

                        INVENTORY="$WORKSPACE/ansible/inventory.ini"

                        echo "Testing Ansible connectivity..."

                        ansible calculator \
                            -i "$INVENTORY" \
                            -m ping \
                            --private-key="$ANSIBLE_SSH_KEY" \
                            -e "ansible_user=$ANSIBLE_SSH_USER"

                        echo "Installing Java..."

                        ansible-playbook \
                            -i "$INVENTORY" \
                            "$WORKSPACE/ansible/install-java.yml" \
                            --private-key="$ANSIBLE_SSH_KEY" \
                            -e "ansible_user=$ANSIBLE_SSH_USER"

                        echo "Installing Tomcat..."

                        ansible-playbook \
                            -i "$INVENTORY" \
                            "$WORKSPACE/ansible/install-tomcat.yml" \
                            --private-key="$ANSIBLE_SSH_KEY" \
                            -e "ansible_user=$ANSIBLE_SSH_USER"
                    '''
                }
            }
        }

        stage('Deploy to Tomcat via Ansible') {
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'ansible-target-ssh-student',
                        keyFileVariable: 'ANSIBLE_SSH_KEY',
                        usernameVariable: 'ANSIBLE_SSH_USER'
                    ),
                    string(
                        credentialsId: 'ansible-vault-password',
                        variable: 'ANSIBLE_VAULT_PASSWORD'
                    )
                ]) {
                    sh '''
                        set +x
                        set -eu

                        export ANSIBLE_CONFIG="$WORKSPACE/ansible/ansible.cfg"

                        INVENTORY="$WORKSPACE/ansible/inventory.ini"
                        PLAYBOOK="$WORKSPACE/ansible/deploy-calculator.yml"

                        echo "Getting application version..."

                        APP_VERSION="$(mvn -q -DforceStdout \
                            help:evaluate \
                            -Dexpression=project.version)"

                        test -n "$APP_VERSION"
                        test -r "$ANSIBLE_SSH_KEY"

                        echo "Application version: $APP_VERSION"

                        echo "Validating SSH private key..."

                        ssh-keygen -y \
                            -f "$ANSIBLE_SSH_KEY" \
                            >/dev/null

                        echo "Testing Ansible connectivity..."

                        ansible calculator \
                            -i "$INVENTORY" \
                            -m ping \
                            --private-key="$ANSIBLE_SSH_KEY" \
                            -e "ansible_user=$ANSIBLE_SSH_USER"

                        echo "Creating temporary Ansible Vault password file..."

                        VAULT_PASS_FILE="$(mktemp)"

                        trap 'rm -f "$VAULT_PASS_FILE"' EXIT

                        chmod 600 "$VAULT_PASS_FILE"

                        printf '%s' "$ANSIBLE_VAULT_PASSWORD" > "$VAULT_PASS_FILE"

                        unset ANSIBLE_VAULT_PASSWORD

                        echo "Deploying application to Tomcat..."

                        ansible-playbook \
                            -i "$INVENTORY" \
                            "$PLAYBOOK" \
                            --vault-password-file "$VAULT_PASS_FILE" \
                            --private-key="$ANSIBLE_SSH_KEY" \
                            -e "ansible_user=$ANSIBLE_SSH_USER" \
                            -e "artifact_version=$APP_VERSION"

                        echo "Tomcat deployment completed successfully."
                    '''
                }
            }
        }
    }

    post {

        always {
            junit(
                allowEmptyResults: true,
                testResults: '**/target/surefire-reports/*.xml'
            )

            archiveArtifacts(
                artifacts: 'target/*.war',
                fingerprint: true
            )
        }

        success {
            echo 'Build, tests, analysis, deployments, and archiving succeeded.'

            emailext(
                to: 'yeswanthyeswanth23@gmail.com',
                subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """Build succeeded: ${env.JOB_NAME} #${env.BUILD_NUMBER}
Build URL: ${env.BUILD_URL}
Status: ${currentBuild.currentResult}"""
            )

            slackSend(
                channel: '#devops-builds',
                color: 'good',
                message: """SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}
Build URL: ${env.BUILD_URL}"""
            )
        }

        failure {
            echo 'Pipeline failed. Review the Jenkins console output.'

            emailext(
                to: 'yeswanthyeswanth23@gmail.com',
                subject: "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """Build failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}
Build URL: ${env.BUILD_URL}
Please check the Jenkins console output."""
            )

            slackSend(
                channel: '#devops-builds',
                color: 'danger',
                message: """FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}
Build URL: ${env.BUILD_URL}"""
            )
        }
    }
}