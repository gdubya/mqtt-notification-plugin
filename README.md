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

# Changelog
#### 1.8 (2020-10-06)
* Moved docs to Github docs

#### 1.7 (2017-06-27)

The MqttNotifier can now be used in a jenkins pipeline, too. (Thanks
[michaelknigge](https://github.com/michaelknigge/mqtt-notification-plugin/commit/18ff49ab1b4849091060e22113caf7b119cd99cb)!)

#### 1.6 (2017-03-15)

Fixed credentials problem introduced while refactoring for unit testing
in previous release
([JENKINS-42764](https://issues.jenkins-ci.org/browse/JENKINS-42764))

#### 1.5 (2017-03-06)

-   Fixed problem with variable substitution
    ([JENKINS-41974](https://issues.jenkins-ci.org/browse/JENKINS-41974))
-   Started using
    [StrSubstitutor](https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/text/StrSubstitutor.html)
    for variable substitution
-   Added a unit test, but it's not doing much yet. Need to improve
    this.

#### 1.4.2 (2017-02-12)

-   Reverted language level to Java 7 following failed integration test

#### 1.4.1 (2017-02-12)

-   Bumped pom parent version, triggered a few findbugs violations. Now
    fixed.

#### 1.4 (2017-02-12)

-   MQTT Notification plugin should be able to publish build number,
    build parameters
    ([JENKINS-41839](https://issues.jenkins-ci.org/browse/JENKINS-41839))

#### 1.3 (2016-01-09)

-   Environment variables can now be used in topic and message
    ([JENKINS-31669](https://issues.jenkins-ci.org/browse/JENKINS-31669))
-   Guard against credentials leak
    ([JENKINS-25035](https://issues.jenkins-ci.org/browse/JENKINS-25035))
-   Bumped Paho MQTT client version ([GitHub issue
    4](https://github.com/gdubya/mqtt-notification-plugin/issues/4))

#### 1.2.1 (2016-01-06)

-   No changes. Republished 1.2 as the release didn't quite make it
    public for some reason.

#### 1.2 (2015-06-23)

-   I'm not dead yet!
-   Bumped jenkins-ci plugin parent to 1.600
    ([JENKINS-23239](https://issues.jenkins-ci.org/browse/JENKINS-23239))
-   Merged pull request to [use credentials when testing broker
    connection](https://github.com/jenkinsci/mqtt-notification-plugin/pull/2)

#### 1.1 (2013-08-15)

-   Modified DefaultFilePersistence to use java.io.tmpdir

#### 1.0 (2013-08-12)

-   Initial release
