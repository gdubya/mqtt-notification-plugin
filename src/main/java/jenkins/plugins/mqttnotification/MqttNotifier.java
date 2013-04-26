package jenkins.plugins.mqttnotification;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.User;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.PrintStream;
import java.net.URISyntaxException;

/**
 * A simple build result notifier that publishes the result via MQTT.
 *
 * @author Gareth Western
 */
public class MqttNotifier extends Notifier {

    private static final String DISPLAY_NAME = "MQTT Notification";

    private static final String DEFAULT_TOPIC = "jenkins/$PROJECT_URL";
    private static final String DEFAULT_MESSAGE = "$BUILD_RESULT";

    private final String brokerHost;

    private final int brokerPort;

    private final String topic;

    private final String message;

    private final String qos;

    private final boolean retainMessage;

    @DataBoundConstructor
    public MqttNotifier(String brokerHost, int brokerPort, String topic, String message, String qos, boolean retainMessage) {
        this.brokerHost = brokerHost;
        this.brokerPort = brokerPort;
        this.topic = topic;
        this.message = message;
        this.qos = qos;
        this.retainMessage = retainMessage;
    }

    public String getBrokerHost() {
        return brokerHost;
    }

    public int getBrokerPort() {
        return brokerPort;
    }

    public String getTopic() {
        return StringUtils.isEmpty(topic) ? DEFAULT_TOPIC : topic;
    }

    public String getMessage() {
        return StringUtils.isEmpty(message) ? DEFAULT_MESSAGE : message;
    }

    public QoS getQos() {
        return QoS.valueOf(qos);
    }

    public boolean isRetainMessage() {
        return retainMessage;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {

        final PrintStream logger = listener.getLogger();
        final MQTT mqtt = new MQTT();
        try {
            mqtt.setHost(getBrokerHost(), getBrokerPort());
            final CallbackConnection connection = mqtt.callbackConnection();
            connection.connect(new Callback<Void>() {
                public void onFailure(Throwable value) {
                    logger.println("ERROR: Failed to connect to MQTT broker: " + value.getMessage());
                }

                public void onSuccess(Void v) {
                    connection.publish(
                            replaceVariables(getTopic(), build),
                            replaceVariables(getMessage(), build).getBytes(),
                            getQos(),
                            isRetainMessage(),
                            new Callback<Void>() {
                                public void onSuccess(Void v) {
                                    // noop
                                }

                                public void onFailure(Throwable value) {
                                    logger.println("ERROR: Failed to publish MQTT notification: " + value.getMessage());
                                }
                            });

                    connection.disconnect(new Callback<Void>() {
                        public void onSuccess(Void v) {
                            // noop
                        }

                        public void onFailure(Throwable value) {
                            // noop
                        }
                    });
                }
            });
        } catch (URISyntaxException use) {
            logger.println("ERROR: Caught URISyntaxException while configuring MQTT connection: " + use.getMessage());
        }
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public FormValidation doCheckBrokerHost(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        public FormValidation doCheckBrokerPort(@QueryParameter String value) {
            return FormValidation.validatePositiveInteger(value);
        }

        public ListBoxModel doFillQosItems() {
            ListBoxModel items = new ListBoxModel();
            for (QoS qos : QoS.values()) {
                // Disabled until issue is resolved: https://github.com/fusesource/mqtt-client/issues/17
                // If this QoS is requested then we'll use a different MQTT client library
                if (qos != QoS.EXACTLY_ONCE) {
                    items.add(qos.name(), qos.toString());
                }
            }
            return items;
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return DISPLAY_NAME;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req,formData);
        }
    }

    private String replaceVariables(final String rawString, final AbstractBuild build) {
        String result = rawString.replaceAll("\\$PROJECT_URL", build.getProject().getUrl());
        result = result.replaceAll("\\$BUILD_RESULT", build.getResult().toString());
        if (rawString.contains("$CULPRITS")) {
            StringBuilder culprits = new StringBuilder();
            String delim = "";
            for (Object userObject : build.getCulprits()) {
                culprits.append(delim).append(userObject.toString());
                delim = ",";
            }
            result = result.replaceAll("\\$CULPRITS", culprits.toString());
        }
        return result;
    }

}
