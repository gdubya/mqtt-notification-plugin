package com.garethwestern.jenkins.plugins.mqttnotification;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.PrintStream;

/**
 * A simple notifier
 */
public class MqttNotifier extends Notifier {

    private final String brokerHost;

    private final int brokerPort;

    private final String topic;

    private final String message;

    private final String qos;

    @DataBoundConstructor
    public MqttNotifier(String brokerHost, int brokerPort, String topic, String message, String qos) {
        this.brokerHost = brokerHost;
        this.brokerPort = brokerPort;
        this.topic = topic;
        this.message = message;
        this.qos = qos;
    }

    public String getBrokerHost() {
        return brokerHost;
    }

    public int getBrokerPort() {
        return brokerPort;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessage() {
        return message;
    }

    public String getQos() {
        return qos;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        PrintStream logger = listener.getLogger();

        String topic = "jenkins/" + build.getProject().getUrl();

        Result result = build.getResult();
        try {
            MQTT mqtt = new MQTT();
            mqtt.setHost("localhost", 1883);
            BlockingConnection connection = mqtt.blockingConnection();
            connection.connect();
            connection.publish(topic, result.toString().getBytes(), QoS.AT_LEAST_ONCE, false);
            connection.disconnect();
        } catch (Exception e) {
            logger.println("ERROR: " + e.getMessage());
            throw new IOException(e);
        }

        return true;
    }

    @Extension
    public static final class DescriptorImpl
            extends BuildStepDescriptor<Publisher> {

        public FormValidation doCheckBrokerHost(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        public FormValidation doCheckBrokerPort(@QueryParameter String value) {
            return FormValidation.validatePositiveInteger(value);
        }

        public FormValidation doCheckTopic(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        public FormValidation doCheckMessage(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        public FormValidation doCheckQos(@QueryParameter String value) {
            FormValidation result =  FormValidation.validatePositiveInteger(value);
            if (result == FormValidation.ok()) {
                int intValue = Integer.valueOf(value);
                if (intValue > 3) {
                    result = FormValidation.error("QOS must be either 1, 2, or 3");
                }
            }
            return result;
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "MQTT Notification";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req,formData);
        }
    }

}
