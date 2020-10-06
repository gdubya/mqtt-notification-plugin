MQTT Notification plugin
====================

[![Join the chat at https://gitter.im/jenkinsci/mqtt-notification-plugin](https://badges.gitter.im/jenkinsci/role-strategy-plugin.svg)](https://gitter.im/jenkinsci/role-strategy-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/mqtt-notification-plugin.svg)](https://plugins.jenkins.io/mqtt-notification-plugin)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/mqtt-notification-plugin.svg?label=changelog)](https://github.com/jenkinsci/mqtt-notification-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/mqtt-notification-plugin.svg?color=blue)](https://plugins.jenkins.io/mqtt-notification-plugin)

# About

A simple notifier that can publish build notifications to a topic on
a [MQTT](http://mqtt.org/) broker.

[MQTT](http://mqtt.org/) is a machine-to-machine (M2M)/"Internet of
Things" connectivity protocol. It was designed as an extremely
lightweight publish/subscribe messaging transport.

The following details are configurable per Jenkins job:

-   Broker hostname/port
-   Topic
-   Message

Both the topic and the message may also include certain dynamic
variables. These include:

-   PROJECT\_URL - The relative URL to the Jenkins project to which this
    job belongs (e.g. "job/my-build").
-   BUILD\_RESULT - The result of the Job (e.g. SUCCESS, FAILURE,
    ABORTED, etc.)
-   BUILD\_NUMBER - The build number
-   CULPRITS - Comma-separated list of build culprits

All other build variables and environment variables can also be
used ([JENKINS-41839](https://issues.jenkins-ci.org/browse/JENKINS-41839)).

The default topic when no value is specified is "jenkins/PROJECT\_URL".

The default message when no value is specified is "BUILD\_RESULT".
