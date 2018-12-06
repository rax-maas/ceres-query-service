def label = "maven-${UUID.randomUUID().toString()}"
def name = 'docker-credential-gcr'
def fp = "/usr/bin/${name}"
def url = 'https://github.com/GoogleCloudPlatform/docker-credential-gcr/releases/download/v1.5.0/docker-credential-gcr_linux_amd64-1.5.0.tar.gz'

podTemplate(label: label, containers: [
  containerTemplate(name: 'maven', image: 'maven:3-jdk-8', ttyEnabled: true, command: 'cat')
])

{
  node(label) {
    container('maven') {
      stage('checkout') {
        checkout scm
      }
      ansiColor('xterm') {
        stage('build') {
          withCredentials([file(credentialsId: 'ceres-jenkins-gcr', variable: 'GOOGLE_APPLICATION_CREDENTIALS')]) {
            sh "curl -fsSL ${url} | tar -xzO ./${name} > ${fp} && chmod +x ${fp}"
            sh 'mvn -DskipTests -P docker -Ddocker.image.prefix=gcr.io/ceres-dev-222017 package'
          }
        }
      }
    }
  }
}
