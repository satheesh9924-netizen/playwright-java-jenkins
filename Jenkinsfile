pipeline {
    agent any
    environment {
        COMPOSE_PROJECT_NAME = "playwright-java-jenkins-${env.BUILD_NUMBER}"
    }
    triggers {
        githubPush()
    }
    stages {
        stage('Checkout') {
            steps { checkout scm }
        }
        stage('Verify Docker Environment') {
            steps {
                sh '''
                    command -v docker >/dev/null 2>&1 || { echo "ERROR: docker CLI not found on this agent. Install Docker and ensure the Jenkins user can reach /var/run/docker.sock."; exit 1; }
                    docker version >/dev/null 2>&1 || { echo "ERROR: docker CLI found but cannot reach the daemon. Add the Jenkins user to the 'docker' group (or grant socket access) and restart the agent."; exit 1; }
                    docker compose version >/dev/null 2>&1 || { echo "ERROR: 'docker compose' (Compose v2 plugin) not available. Install the Docker Compose plugin -- the legacy docker-compose v1 binary will not work here."; exit 1; }
                '''
            }
        }
        stage('Build & Run Tests (Docker)') {
            steps {
                sh 'mkdir -p target'
                sh 'docker compose build'
                sh 'docker compose run --rm playwright-java-tests'
            }
        }
    }
    post {
        always {
            sh 'docker run --rm -v "$(pwd)/target:/target" alpine chown -R "$(id -u):$(id -g)" /target || true'
            sh 'docker compose down --volumes --remove-orphans || true'
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: 'target/screenshots/*.png, target/traces/*.zip, target/surefire-reports/**', allowEmptyArchive: true
        }
    }
}
