pipeline {
    agent any

    environment {
        PROJECT_KEY = 'ms-auth-prueba'
        PROJECT_NAME = 'ms-auth-prueba'
        JACOCO_XML = 'target/site/jacoco/jacoco.xml'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test (JaCoCo)') {
            steps {
                sh '''
                    echo "Ejecutando compilacion, pruebas y verificacion con Maven..."
                    if [ -f "./mvnw" ]; then
                      chmod +x ./mvnw
                      ./mvnw -B clean verify
                    else
                      mvn -B clean verify
                    fi

                    echo "Verificando existencia del reporte JaCoCo..."
                    if [ -f "target/site/jacoco/jacoco.xml" ]; then
                      echo "Reporte JaCoCo generado correctamente."
                      ls -lh target/site/jacoco/jacoco.xml
                    else
                      echo "ERROR: No se encontro target/site/jacoco/jacoco.xml"
                      exit 1
                    fi
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh '''
                        echo "Ejecutando analisis SonarQube para ms-auth-prueba..."
                        if [ -f "./mvnw" ]; then
                          chmod +x ./mvnw
                          ./mvnw -B sonar:sonar \
                            -Dsonar.projectKey=$PROJECT_KEY \
                            -Dsonar.projectName=$PROJECT_NAME \
                            -Dsonar.coverage.jacoco.xmlReportPaths=$JACOCO_XML
                        else
                          mvn -B sonar:sonar \
                            -Dsonar.projectKey=$PROJECT_KEY \
                            -Dsonar.projectName=$PROJECT_NAME \
                            -Dsonar.coverage.jacoco.xmlReportPaths=$JACOCO_XML
                        fi
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 2, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate abortPipeline: true
                        echo "Resultado Quality Gate: ${qg.status}"
                    }
                }
            }
        }

        stage('Package') {
            steps {
                sh '''
                    echo "Empaquetando microservicio sin volver a ejecutar pruebas..."
                    echo "No se usa clean aqui para conservar los reportes generados previamente."

                    if [ -f "./mvnw" ]; then
                      chmod +x ./mvnw
                      ./mvnw -B package -DskipTests
                    else
                      mvn -B package -DskipTests
                    fi
                '''
            }
        }

        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
                junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
            }
        }
    }

    post {
        always {
            echo 'Pipeline backend ms-auth-prueba finalizado.'
        }
        success {
            echo 'Microservicio ms-auth-prueba compilado, probado, analizado en SonarQube y empaquetado correctamente.'
        }
        failure {
            echo 'El pipeline fallo. Revisar Console Output para identificar la etapa exacta.'
        }
    }
}
