pipeline {
    agent any

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-21-amazon-corretto.x86_64'
        PATH = "${JAVA_HOME}/bin:${PATH}"
        SONAR_HOST_URL = 'http://sonar.leancloudandevops.online:9000'
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
            }
        }

        stage('Build and Test') {
            steps {
                sh 'mvn -B clean verify'
            }
        }


        stage('SonarQube Analysis') {
    steps {
        withCredentials([
            string(
                credentialsId: 'sonar-token',
                variable: 'SONAR_TOKEN'
            )
        ]) {
            sh '''
                mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                  -Dsonar.projectKey=DevOps-Calculator-Web-Yashwanth \
                  -Dsonar.projectName="DevOps Calculator Web - Yashwanth" \
                  -Dsonar.host.url="$SONAR_HOST_URL" \
                  -Dsonar.token="$SONAR_TOKEN"
            '''
        }
    }
}


        stage('Verify WAR') {
            steps {
                sh '''
                    test -f target/calculator.war
                    ls -lh target/calculator.war
                '''
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true,
                  testResults: '**/target/surefire-reports/*.xml'

            archiveArtifacts artifacts: 'target/*.war',
                             fingerprint: true
        }

        success {
            echo 'Build, JUnit tests, WAR verification, and artifact archiving succeeded.'
        }

        failure {
            echo 'Pipeline failed. Review the Jenkins console output.'
        }
    }
}
