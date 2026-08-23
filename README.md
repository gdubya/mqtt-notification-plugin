MQTT Notification plugin
====================

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/mqtt-notification-plugin.svg)](https://plugins.jenkins.io/mqtt-notification-plugin)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/mqtt-notification-plugin.svg?label=changelog)](https://github.com/jenkinsci/mqtt-notification-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/mqtt-notification-plugin.svg?color=blue)](https://plugins.jenkins.io/mqtt-notification-plugin)

# About

A simple notifier that can publish build notifications to a topic on
a [MQTT](http://mqtt.org/) broker.

[MQTT](http://mqtt.org/) is a machine-to-machine (M2M)/"Internet of
Things" connectivity protocol. It was designed as an extremely
lightweight publish/subscribe messaging transport.

# Configuration
The following details are configurable per Jenkins job:

-   **Broker URL** - The URL to the MQTT broker (e.g. `tcp://localhost:1883` or `ssl://broker.example.com:8883`).
-   **Credentials** - Optional username/password credentials configured in Jenkins.
-   **Topic** - Topic to publish notifications to (default: `jenkins/$PROJECT_URL`).
-   **Message** - Message payload (default: `$BUILD_RESULT`).
-   **Notify on build start?** - Optional toggle to send an MQTT notification when the build begins execution.
-   **Start Topic** - Optional topic specifically for build start notifications (falls back to Topic).
-   **Start Message** - Optional payload for build start notifications (default: `STARTED`).
-   **Notify on build completion?** - Toggle to send notification upon build completion (default: true).
-   **Quality of Service** - QoS level (0 - At most once, 1 - At least once, 2 - Exactly once).
-   **Retain Message?** - Whether to set the retain flag on published messages.

Both topic and message strings support dynamic variable replacement:

-   `PROJECT_URL` - The relative URL to the Jenkins project (e.g. "job/my-build/").
-   `BUILD_RESULT` - The result of the build (e.g. `SUCCESS`, `FAILURE`, `ABORTED`, etc.).
-   `BUILD_NUMBER` - The build number.
-   `BUILD_DISPLAY_NAME` - The display name of the build (e.g. `#1` or custom name).
-   `CULPRITS` - Comma-separated list of build culprits.
-   All standard Jenkins environment variables and build parameters.

# Pipeline Usage

The plugin provides the `mqttNotification` step for use in Jenkins Pipelines.

### Declarative Pipeline Example

```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                echo 'Building...'
            }
        }
    }
    post {
        always {
            mqttNotification(
                brokerUrl: 'tcp://localhost:1883',
                topic: 'jenkins/builds/${JOB_NAME}',
                message: '{ "job": "${JOB_NAME}", "build": "${BUILD_NUMBER}", "status": "${BUILD_RESULT}" }',
                qos: '0',
                retainMessage: false,
                credentialsId: 'my-mqtt-credentials' // optional
            )
        }
    }
}
```

### Build Start Notification Example

```groovy
stage('Notify Start') {
    steps {
        mqttNotification(
            brokerUrl: 'tcp://localhost:1883',
            topic: 'jenkins/builds/${JOB_NAME}',
            message: 'Build ${BUILD_DISPLAY_NAME} started'
        )
    }
}
```

### Scripted Pipeline Example

```groovy
node {
    stage('Build') {
        mqttNotification brokerUrl: 'tcp://localhost:1883', topic: 'jenkins/start', message: 'Build started'
        // ... build steps ...
        mqttNotification brokerUrl: 'tcp://localhost:1883', topic: 'jenkins/finish', message: 'Build finished: ${BUILD_RESULT}'
    }
}
```

# License
This Jenkins plugin is licensed under the [MIT License](./LICENSE.txt).