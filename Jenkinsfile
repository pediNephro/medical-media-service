pipeline {
    agent any

    tools {
        maven 'maven'
    }

    environment {
        IMAGE_NAME = "alaadid/medical-media-service"
    }

    stages {
stage('Build & Test') {
    steps {
        sh 'mvn clean verify'
    }
}

        stage('SonarQube') {
            steps {
                withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                    sh '''
                    mvn sonar:sonar \
                    -Dsonar.projectKey=medical-media-service \
                    -Dsonar.host.url=http://192.168.56.10:9000 \
                    -Dsonar.login=$SONAR_TOKEN \
                    -DskipTests
                    '''
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker build -t $IMAGE_NAME:latest .'
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'USER',
                    passwordVariable: 'PASS'
                )]) {
                    sh '''
                    echo $PASS | docker login -u $USER --password-stdin
                    docker push $IMAGE_NAME:latest
                    '''
                }
            }
        }

        stage('Trigger CD') {
            steps {
                build job: 'medical-media-cd'
            }
        }
    }
}
