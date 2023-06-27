pipeline {
    agent any

    environment {
        AWS_DEFAULT_REGION = 'ap-south-1'
        JENKINS_PRIVATE_KEY = credentials('jenkins-dev-private-key')
		SSH_PORT = credentials('dev-instance-ssh-port')
        ECR_USERNAME = credentials('aws-dev-ecr-user')
        ECR_URL = credentials('aws-dev-ecr-url')
        TEAMS_WEBHOOK_URL = credentials('teams-webhook-url')
        GITHUB_USERNAME = credentials('ghcr_username')
        GITHUB_TOKEN = credentials('ghcr_password')
    }

    options {
        skipDefaultCheckout()
    }

    stages {
        stage ('Checkout') {
            steps {
                echo "Branch name: ${BRANCH_NAME}"
                echo "Commit id: ${COMMIT_ID}"
                echo "Commit message: ${COMMIT_MESSAGE}"
                checkout scmGit(branches: [[name: "${BRANCH_NAME}"]], extensions: [], userRemoteConfigs: [[credentialsId: 'cogo-dev-github-app', url: 'https://github.com/Cogoport/ares.git']])
            }
        }

        stage('Build') {
            when {
                expression { sh (script: "git log -1 --pretty=%B ${COMMIT_ID}", returnStdout: true).contains('#deploy_on') }
            }

            steps {
                office365ConnectorSend webhookUrl: "${TEAMS_WEBHOOK_URL}", message: "## Starting build for commit *${COMMIT_ID}* of branch **${BRANCH_NAME}** for user **${PUSHER_NAME}**", color: '#3366ff'

                sh "aws ecr get-login-password --region ap-south-1 | docker login --username ${ECR_USERNAME} --password-stdin ${ECR_URL}"

                script {
                    sh "MICRONAUT_ENVIRONMENTS=dev ./gradlew build -x test"
                    sh "docker image build -t ares:current ."
                    sh "docker image tag ares:current ${ECR_URL}/ares:${COMMIT_ID}"
                    sh "docker image push ${ECR_URL}/ares:${COMMIT_ID}"
                }
            }
        }
        stage('Deploy') {
            when {
                expression { sh (script: "git log -1 --pretty=%B ${COMMIT_ID}", returnStdout: true).contains('#deploy_on') }
            }

            steps {
                echo 'Deploying ares...'

                script {
                    SERVER_NAME = sh (script: "git log -1 --pretty=%B ${COMMIT_ID} | awk \'{print \$NF}\'", returnStdout:true).trim()
                }

                // get private IP of server
                script {
                    SERVER_IP = sh (script: "aws ec2 describe-instances --filters \"Name=tag:Name,Values=${SERVER_NAME}\" --query \"Reservations[*].Instances[*].PrivateIpAddress\" --output text", returnStdout:true).trim()
                }

                echo "IP for server ${SERVER_NAME} found ${SERVER_IP}"

                // SSH into server IP and run deploy commands
                sh """ssh -o StrictHostKeyChecking=no -i ${env.JENKINS_PRIVATE_KEY} ${SERVER_NAME}@${SERVER_IP} -p ${SSH_PORT} \" sed -i \'/^ARES_TAG/s/=.*\$/=${COMMIT_ID}/g\' /home/${SERVER_NAME}/.env && \
                aws ecr get-login-password --region ap-south-1 | docker login --username ${ECR_USERNAME} --password-stdin ${ECR_URL} && \
                docker compose --env-file /home/${SERVER_NAME}/.env -f /home/${SERVER_NAME}/docker-compose.yaml up ares -d
                \""""
            }

            post {
                success {
                    office365ConnectorSend webhookUrl: "${TEAMS_WEBHOOK_URL}", message: "## Successfully deployed  commit *${COMMIT_ID}* of **${BRANCH_NAME}** for user **${PUSHER_NAME}** on server ${SERVER_NAME}", color:  '#66ff66'
                }

                failure {
                    office365ConnectorSend webhookUrl: "${TEAMS_WEBHOOK_URL}", message: "## Failed to deploy commit id *${COMMIT_ID}* of **${BRANCH_NAME}** for user **${PUSHER_NAME}** on server ${SERVER_NAME}", color: '#ff0000'
                }
            }

        }
    }
}