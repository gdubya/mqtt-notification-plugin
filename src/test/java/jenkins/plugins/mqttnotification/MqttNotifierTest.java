package jenkins.plugins.mqttnotification;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Shell;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MqttNotifierTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private MqttNotifier createSubject() {
        return new MqttNotifier(
            "tcp://localhost:1883",
            "jenkins/$JOB_NAME",
            "$BUILD_RESULT",
            "1",
            false,
            null
        );
    }

    @Test
    public void canInitiateNewNotifier() {
        MqttNotifier notifier = createSubject();
        assertNotNull(notifier);
    }

    @Test
    public void canGetAllFields() {
        MqttNotifier notifier = createSubject();
        assertEquals("jenkins/$JOB_NAME", notifier.getTopic());
        assertEquals("$BUILD_RESULT", notifier.getMessage());
        assertEquals("tcp://localhost:1883", notifier.getBrokerUrl());
        assertEquals("1", notifier.getQos());
        assertEquals(BuildStepMonitor.NONE, notifier.getRequiredMonitorService());
        assertEquals(false, notifier.isNotifyOnStart());
        assertEquals(true, notifier.isNotifyOnComplete());
        assertEquals("jenkins/$JOB_NAME", notifier.getEffectiveStartTopic());
        assertEquals("STARTED", notifier.getEffectiveStartMessage());

        notifier.setNotifyOnStart(true);
        notifier.setNotifyOnComplete(false);
        notifier.setStartTopic("jenkins/start/$JOB_NAME");
        notifier.setStartMessage("Job $JOB_NAME started");

        assertEquals(true, notifier.isNotifyOnStart());
        assertEquals(false, notifier.isNotifyOnComplete());
        assertEquals("jenkins/start/$JOB_NAME", notifier.getStartTopic());
        assertEquals("jenkins/start/$JOB_NAME", notifier.getEffectiveStartTopic());
        assertEquals("Job $JOB_NAME started", notifier.getStartMessage());
        assertEquals("Job $JOB_NAME started", notifier.getEffectiveStartMessage());
    }

    @Test
    public void testReplaceVariablesWithDefaultDisplayName() throws Exception {
        MqttNotifier notifier = createSubject();
        FreeStyleProject project = j.createFreeStyleProject("test-default-name");
        FreeStyleBuild build = project.scheduleBuild2(0).get();

        String template = "{ \"job\": \"${JOB_NAME}\", \"name\": \"${BUILD_DISPLAY_NAME}\", \"state\": \"${BUILD_RESULT}\" }";
        String replaced = notifier.replaceVariables(template, build, TaskListener.NULL);
        System.out.println("Replaced default: " + replaced);
        assertEquals("{ \"job\": \"test-default-name\", \"name\": \"#1\", \"state\": \"SUCCESS\" }", replaced);
    }

    @Test
    public void testReplaceVariablesWithCustomDisplayName() throws Exception {
        MqttNotifier notifier = createSubject();
        FreeStyleProject project = j.createFreeStyleProject("test-custom-name");
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        build.setDisplayName("CustomBuildName-1.0.0");

        String template = "{ \"job\": \"${JOB_NAME}\", \"name\": \"${BUILD_DISPLAY_NAME}\", \"state\": \"${BUILD_RESULT}\" }";
        String replaced = notifier.replaceVariables(template, build, TaskListener.NULL);
        System.out.println("Replaced custom: " + replaced);
        assertEquals("{ \"job\": \"test-custom-name\", \"name\": \"CustomBuildName-1.0.0\", \"state\": \"SUCCESS\" }", replaced);
    }

    @Test
    public void testBuildStartNotificationExecution() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("test-start-job");
        MqttNotifier notifier = createSubject();
        notifier.setNotifyOnStart(true);
        notifier.setNotifyOnComplete(true);
        notifier.setStartTopic("jenkins/start/$JOB_NAME");
        notifier.setStartMessage("Build #${BUILD_NUMBER} started for ${JOB_NAME}");
        project.getPublishersList().add(notifier);

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertEquals(hudson.model.Result.SUCCESS, build.getResult());

        String startTopicReplaced = notifier.replaceVariables(notifier.getEffectiveStartTopic(), build, TaskListener.NULL);
        String startMsgReplaced = notifier.replaceVariables(notifier.getEffectiveStartMessage(), build, TaskListener.NULL);
        assertEquals("jenkins/start/test-start-job", startTopicReplaced);
        assertEquals("Build #1 started for test-start-job", startMsgReplaced);
    }

    public void setEnvironmentVariables() throws IOException {
        EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
        EnvVars envVars = prop.getEnvVars();
        envVars.put("sampleEnvVarKey", "sampleEnvVarValue");
        j.jenkins.getGlobalNodeProperties().add(prop);
    }
}
