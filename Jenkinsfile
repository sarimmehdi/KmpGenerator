pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
        timestamps()
    }

    environment {
        GRADLE_USER_HOME = "${WORKSPACE}/.gradle"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Prepare') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew --version'
            }
        }

        stage('Unit Tests') {
            steps {
                sh './gradlew :kmp-generator-plugin:test jacocoTestReport --build-cache --no-daemon'
            }
            post {
                always {
                    junit 'kmp-generator-plugin/build/test-results/test/*.xml'

                    script {
                        try {
                            jacoco(
                                execPattern: '**/build/jacoco/*.exec',
                                classPattern: '**/build/classes/kotlin/main',
                                sourcePattern: '**/src/main/kotlin',
                                inclusionPattern: '**/com/sarimmehdi/**'
                            )
                        } catch (Exception e) {
                            echo "JaCoCo plugin not found. Skipping coverage recording."
                        }
                    }
                }
            }
        }

        stage('Static Analysis') {
            steps {
                sh './gradlew detekt ktlintCheck --build-cache --no-daemon'
            }
            post {
                always {
                    recordIssues(
                        tool: detekt(pattern: '**/reports/detekt/*.xml'),
                        id: 'detekt',
                        name: 'Detekt'
                    )
                    recordIssues(
                        tool: checkStyle(pattern: '**/reports/ktlint/*.xml'),
                        id: 'ktlint',
                        name: 'KtLint'
                    )
                }
            }
        }
    }
}