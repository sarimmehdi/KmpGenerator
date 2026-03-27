pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Prepare') {
            steps {
                sh 'chmod +x gradlew'
            }
        }

        stage('Unit Tests') {
            steps {
                sh './gradlew :kmp-generator-plugin:test --no-daemon'
            }
            post {
                always {
                    junit 'kmp-generator-plugin/build/test-results/test/*.xml'
                }
            }
        }

        stage('Static Analysis') {
            steps {
                sh './gradlew :kmp-generator-plugin:ktlintCheck :kmp-generator-plugin:detekt --no-daemon || true'
            }
            post {
                always {
                    recordIssues(
                        enabledForFailure: true,
                        aggregatingResults: true,
                        tools: [
                            detekt(pattern: '**/build/reports/detekt/detekt-*.xml'),
                            checkStyle(pattern: '**/build/reports/ktlint/*.xml', name: 'KtLint')
                        ]
                    )

                    archiveArtifacts artifacts: '**/build/reports/**/*.html', allowEmptyArchive: true
                }
            }
        }
    }
}