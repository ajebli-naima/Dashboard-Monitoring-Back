def version = ''

pipeline {

    environment {
        registry = "registry.local.com:5000"
    }

    agent any

    tools {
         maven "maven"
        jdk "jdk8"

    }

    stages {

        stage('Build') {
            steps {
                configFileProvider([configFile(fileId: 'leyton-maven-nexus-config', variable: 'MAVEN_SETTINGS_XML')]) {
                    sh '''
                    echo "Build java - Begin"
                    mvn -U --batch-mode -s $MAVEN_SETTINGS_XML clean install -DskipTests
                    echo "Build java - End"
                    '''
                }
            }
        }

        stage('Building image') {
            steps {
                script {
                    version = sh(returnStdout: true, script: 'mvn -q -Dexec.executable=echo -Dexec.args=\'${project.version}\' --non-recursive exec:exec');
                    env.VERSION = version.trim();
                    sh 'printenv | grep VERSION';
                    currentBuild.description = "dashboard-back:${VERSION}-${BUILD_NUMBER}";
                }
                sh '''
                echo "Building image - Begin"
                sudo docker build -t dashboard-back:${VERSION}-${BUILD_NUMBER} .
                sudo docker tag dashboard-back:${VERSION}-${BUILD_NUMBER} ${registry}/devops/dashboard/dashboard-back:${VERSION}-${BUILD_NUMBER}
                echo "Building image - End"
                '''
            }
        }

        stage('Push to Registry') {
            steps {
                sh '''
                echo "Push to Registry - Begin"
                sudo docker push ${registry}/devops/dashboard/dashboard-back:${VERSION}-${BUILD_NUMBER}
                echo "Push to Registry - End"
                '''
            }
        }

        stage('Remove Unused docker image') {
            steps {
                sh '''
                echo "Remove Unused docker image - Begin"
                sudo docker rmi -f $(sudo docker images --filter=reference="*dashboard-back:${VERSION}-${BUILD_NUMBER}*" -q)
                echo "Remove Unused docker image - End"
                '''
            }
        }

    }

    post {
        always {
            echo 'One way or another, I have finished'
            deleteDir() /* clean up our workspace */
        }
        success {
            echo 'I succeeeded!'
        }
        unstable {
            echo 'I am unstable :/'
        }
        failure {
            echo 'I failed :('
        }
    }

}
