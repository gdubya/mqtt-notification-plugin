package jenkins.plugins.mqttnotification;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.tasks.BuildStepMonitor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * TODO Learn how to test the plugin properly.
 */
public class MqttNotifierTest {

    private MqttNotifier createSubject() {
        return new MqttNotifier(
            "brokerUrl",
            "topic1",
            "message1",
            "1",
            false,
            (StandardUsernamePasswordCredentials) null
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
        assertEquals("topic1", notifier.getTopic());
        assertEquals("message1", notifier.getMessage());
        assertEquals("brokerUrl", notifier.getBrokerUrl());
        assertEquals(1, notifier.getQos());
        assertEquals(BuildStepMonitor.NONE, notifier.getRequiredMonitorService());
    }

}