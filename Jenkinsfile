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
                    -Dsonar.login=$SONAR_TOKEN
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

        stage('Update Kubernetes Repo') {

            steps {

                withCredentials([usernamePassword(
                    credentialsId: 'github-creds',
                    usernameVariable: 'GIT_USER',
                    passwordVariable: 'GIT_PASS'
                )]) {

                    sh '''
                    rm -rf kubernetes-config

                    git clone https://$GIT_USER:$GIT_PASS@github.com/pediNephro/kubernetes-config.git

                    cd kubernetes-config

                    sed -i "s|alaadid/medical-media-service:.*|alaadid/medical-media-service:latest|g" medical-media.yaml

                    git config user.email "jenkins@ci.com"
                    git config user.name "Jenkins"

                    git add medical-media.yaml

                    git commit -m "update medical media image" || true

                    git push || true
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
