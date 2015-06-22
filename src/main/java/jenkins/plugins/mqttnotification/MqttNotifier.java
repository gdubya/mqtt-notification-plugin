package jenkins.plugins.mqttnotification;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * A simple build result notifier that publishes the result via MQTT.
 *
 * @author Gareth Western
 */
public class MqttNotifier extends Notifier {

    private static final String CLIENT_ID = MqttNotifier.class.getSimpleName();

    private static final String DISPLAY_NAME = "MQTT Notification";

    private static final String DEFAULT_TOPIC = "jenkins/$PROJECT_URL";
    private static final String DEFAULT_MESSAGE = "$BUILD_RESULT";

    private final String brokerUrl;

    private final String topic;

    private final String message;

    private final String qos;

    private final boolean retainMessage;

    private final String credentialsId;

    private StandardUsernamePasswordCredentials credentials;

    private enum Qos {
        AT_MOST_ONCE(0),
        AT_LEAST_ONCE(1),
        EXACTLY_ONCE(2);

        private int value;

        Qos(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @DataBoundConstructor
    public MqttNotifier(String brokerUrl, String topic, String message, String qos, boolean retainMessage, String credentialsId) {
        this(brokerUrl, topic, message, qos, retainMessage, lookupSystemCredentials(credentialsId));
    }

    public MqttNotifier(String brokerUrl, String topic, String message, String qos, boolean retainMessage,
                        StandardUsernamePasswordCredentials credentials) {
        this.brokerUrl = brokerUrl;
        this.topic = topic;
        this.message = message;
        this.qos = qos;
        this.retainMessage = retainMessage;
        this.credentials = credentials;
        this.credentialsId = credentials == null ? null : credentials.getId();
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public String getTopic() {
        return StringUtils.isEmpty(topic) ? DEFAULT_TOPIC : topic;
    }

    public String getMessage() {
        return StringUtils.isEmpty(message) ? DEFAULT_MESSAGE : message;
    }

    public int getQos() {
        return Integer.valueOf(qos);
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

    public static StandardUsernamePasswordCredentials lookupSystemCredentials(String credentialsId) {
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StandardUsernamePasswordCredentials.class,
                        Jenkins.getInstance(),
                        ACL.SYSTEM,
                        new ArrayList<DomainRequirement>()
                ),
                CredentialsMatchers.withId(credentialsId)
        );
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        final PrintStream logger = listener.getLogger();
        try {
            final String tmpDir = System.getProperty("java.io.tmpdir");
            final MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);
            final MqttClient mqtt = new MqttClient(getBrokerUrl(), CLIENT_ID, dataStore);
            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            if (this.credentials != null) {
                mqttConnectOptions.setUserName(this.credentials.getUsername());
                mqttConnectOptions.setPassword(this.credentials.getPassword().getPlainText().toCharArray());
            }
            mqtt.connect(mqttConnectOptions);
            mqtt.publish(
                    replaceVariables(getTopic(), build),
                    replaceVariables(getMessage(), build).getBytes(),
                    getQos(),
                    isRetainMessage()
            );
            mqtt.disconnect();
        } catch (final MqttException me) {
            logger.println("ERROR: Caught MqttException while configuring MQTT connection: " + me.getMessage());
            me.printStackTrace(logger);
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
            for (Qos qos : Qos.values()) {
                items.add(qos.name(), String.valueOf(qos.getValue()));
            }
            return items;
        }

        public FormValidation doTestConnection(@QueryParameter("brokerUrl") final String brokerUrl, @QueryParameter("credentialsId") final String credentialsId)
                throws IOException, ServletException {
            if (brokerUrl == null || brokerUrl.trim().isEmpty()) {
                return FormValidation.error("Broker URL must not be empty");
            }
            try {
                final String tmpDir = System.getProperty("java.io.tmpdir");
                final MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);
                final MqttClient mqtt = new MqttClient(brokerUrl, CLIENT_ID, dataStore);

                MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();

                StandardUsernamePasswordCredentials credentials = MqttNotifier.lookupSystemCredentials(credentialsId);

                if (credentials != null) {
                    mqttConnectOptions.setUserName(credentials.getUsername());
                    mqttConnectOptions.setPassword(credentials.getPassword().getPlainText().toCharArray());
                }

                mqtt.connect(mqttConnectOptions);
                mqtt.disconnect();
                return FormValidation.ok("Success");
            } catch (MqttException me) {
                return FormValidation.error(me, "Failed to connect");
            }
        }

        public ListBoxModel doFillCredentialsIdItems() {
            return new StandardUsernameListBoxModel().withEmptySelection().withAll(
                    CredentialsProvider.lookupCredentials(
                            StandardUsernamePasswordCredentials.class,
                            Jenkins.getInstance(),
                            ACL.SYSTEM,
                            new ArrayList<DomainRequirement>()
                    )
            );
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
            return super.configure(req, formData);
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
