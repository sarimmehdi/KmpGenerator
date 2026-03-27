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
                sh './gradlew :kmp-generator-plugin:ktlintCheck :kmp-generator-plugin:detekt --build-cache --no-daemon || true'
            }
            post {
                always {
                    script {
                        try {
                            recordIssues(
                                enabledForFailure: true,
                                aggregatingResults: true,
                                tools: [
                                    detekt(pattern: '**/build/reports/detekt/detekt-*.xml'),
                                    checkStyle(pattern: '**/build/reports/ktlint/*.xml', name: 'KtLint')
                                ]
                            )
                        } catch (Exception e) {
                            echo "Warnings Next Gen plugin missing. Skipping issue recording."
                        }
                    }

                    archiveArtifacts artifacts: '**/build/reports/**/*.html', allowEmptyArchive: true
                }
            }
        }
    }
}