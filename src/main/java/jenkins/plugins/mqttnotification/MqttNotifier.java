package jenkins.plugins.mqttnotification;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.plugins.emailext.plugins.recipients.RecipientProviderUtilities;
import hudson.plugins.emailext.plugins.recipients.RecipientProviderUtilities.IDebug;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

/**
 * A simple build result notifier that publishes the result via MQTT.
 *
 * @author Gareth Western
 */
public class MqttNotifier extends Notifier implements SimpleBuildStep {

    private static final String DISPLAY_NAME = "MQTT Notification";

    private static final String DEFAULT_TOPIC = "jenkins/$PROJECT_URL";
    private static final String DEFAULT_MESSAGE = "$BUILD_RESULT";

    private String brokerUrl;

    private String topic;

    private String message;

    private String qos;

    private boolean retainMessage;

    private String credentialsId;

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
            return this.value;
        }
    }

    @DataBoundConstructor
    public MqttNotifier(
            final String brokerUrl,
            final String topic,
            final String message,
            final String qos,
            final boolean retainMessage,
            final String credentialsId) {

        this.brokerUrl = brokerUrl;
        this.topic = topic;
        this.message = message;
        this.qos = qos;
        this.retainMessage = retainMessage;
        this.credentialsId = credentialsId;
    }

    public String getCredentialsId() {
        return this.credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(final String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getBrokerUrl() {
        return this.brokerUrl;
    }

    @DataBoundSetter
    public void setBrokerUrl(final String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public String getTopic() {
        return isNullOrEmpty(this.topic) ? DEFAULT_TOPIC : this.topic;
    }

    @DataBoundSetter
    public void setTopic(final String topic) {
        this.topic = topic;
    }

    public String getMessage() {
        return isNullOrEmpty(this.message) ? DEFAULT_MESSAGE : this.message;
    }

    @DataBoundSetter
    public void setMessage(final String message) {
        this.message = message;
    }

    public String getQos() {
        return this.qos;
    }

    private int getQualityOfServce() {
        return this.qos == null ? Qos.AT_MOST_ONCE.value : Integer.parseInt(this.qos);
    }

    @DataBoundSetter
    public void setQos(final String qos) {
        this.qos = qos;
    }

    public boolean isRetainMessage() {
        return this.retainMessage;
    }

    @DataBoundSetter
    public void setRetainMessage(final boolean retainMessage) {
        this.retainMessage = retainMessage;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    static StandardUsernamePasswordCredentials lookupSystemCredentials(final String credentialsId) {
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

    private static String getClientId() {
        return MqttNotifier.class.getSimpleName() + UUID.randomUUID().toString();
    }

    @Override
    public void perform(
            final Run<?, ?> run,
            final FilePath workspace,
            final Launcher launcher,
            final TaskListener listener) throws InterruptedException, IOException {

        final PrintStream logger = listener.getLogger();
        final String tmpDir = System.getProperty("java.io.tmpdir");
        final MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);
        try (final MqttClient mqtt = new MqttClient(this.getBrokerUrl(), getClientId(), dataStore)) {
            final MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            final StandardUsernamePasswordCredentials credentials = MqttNotifier
                    .lookupSystemCredentials(this.credentialsId);
            if (credentials != null) {
                mqttConnectOptions.setUserName(credentials.getUsername());
                mqttConnectOptions.setPassword(credentials.getPassword().getPlainText().toCharArray());
            }
            mqtt.connect(mqttConnectOptions);
            mqtt.publish(
                this.replaceVariables(this.getTopic(), run, listener),
                this.replaceVariables(this.getMessage(), run, listener).getBytes(StandardCharsets.UTF_8),
                this.getQualityOfServce(),
                this.isRetainMessage()
            );
            mqtt.disconnect();
        } catch (final MqttException me) {
            logger.println("ERROR: Caught MqttException while configuring MQTT connection: " + me.getMessage());
            me.printStackTrace(logger);
        }
    }

    @Symbol("mqttNotification")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public FormValidation doCheckBrokerHost(@QueryParameter final String value) {
            return FormValidation.validateRequired(value);
        }

        public FormValidation doCheckBrokerPort(@QueryParameter final String value) {
            return FormValidation.validatePositiveInteger(value);
        }

        public ListBoxModel doFillQosItems() {
            final ListBoxModel items = new ListBoxModel();
            for (final Qos qos : Qos.values()) {
                items.add(qos.name(), String.valueOf(qos.getValue()));
            }
            return items;
        }

        public FormValidation doTestConnection(
                @QueryParameter("brokerUrl") final String brokerUrl,
                @QueryParameter("credentialsId") final String credentialsId) {

            if (brokerUrl == null || brokerUrl.trim().isEmpty()) {
                return FormValidation.error("Broker URL must not be empty");
            }

            final String tmpDir = System.getProperty("java.io.tmpdir");
            final MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);
            try (final MqttClient mqtt = new MqttClient(brokerUrl, getClientId(), dataStore)){
                final MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();

                final StandardUsernamePasswordCredentials credentials = MqttNotifier.lookupSystemCredentials(credentialsId);

                if (credentials != null) {
                    mqttConnectOptions.setUserName(credentials.getUsername());
                    mqttConnectOptions.setPassword(credentials.getPassword().getPlainText().toCharArray());
                }

                mqtt.connect(mqttConnectOptions);
                mqtt.disconnect();
                return FormValidation.ok("Success");
            } catch (final MqttException me) {
                return FormValidation.error(me, "Failed to connect");
            }
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath final Item context) {
            return context != null && context.hasPermission(Item.CONFIGURE)
                ? new StandardUsernameListBoxModel()
                .withEmptySelection()
                .withAll(
                    CredentialsProvider.lookupCredentials(
                        StandardUsernamePasswordCredentials.class,
                        context,
                        ACL.SYSTEM,
                        new ArrayList<DomainRequirement>()
                    )
                )
                : new ListBoxModel();
        }

        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            this.save();
            return super.configure(req, formData);
        }
    }

    /**
     * Replace both static and environment variables, build parameters defined in the given rawString
     *
     * @param rawString
     *            The string containing variables to be replaced
     * @param run
     *            The current run
     * @param listener
     *            The current buildListener
     *
     * @return a new String with variables replaced
     *
     * @throws InterruptedException
     * @throws IOException
     */
    private String replaceVariables(final String rawString, final Run<?, ?> run, final TaskListener listener)
            throws IOException, InterruptedException {

        final Result buildResult = run.getResult();
        final EnvVars env = run.getEnvironment(listener);
        if (run instanceof AbstractBuild) {
            env.overrideAll(((AbstractBuild) run).getBuildVariables());
        }

        final StringBuilder culprits = new StringBuilder();
        final Iterator<User> iter = getCulprits(run).iterator();

        while (iter.hasNext()) {
            culprits.append(iter.next().toString());
            if (iter.hasNext()) {
                culprits.append(",");
            }
        }

        // if buildResult is null, we might encounter bug https://issues.jenkins-ci.org/browse/JENKINS-46325
        env.put("BUILD_RESULT", buildResult != null ? buildResult.toString() : "");

        env.put("PROJECT_URL", "job/" + env.get("JOB_NAME") + "/");
        env.put("CULPRITS", culprits.toString());

        return env.expand(rawString);
    }

    // adapted from hudson.plugins.emailext.plugins.recipients.CulpritsRecipientProvider
    private static Set<User> getCulprits(final Run<?, ?> run) {

        if (run instanceof AbstractBuild) {
            return ((AbstractBuild<?, ?>) run).getCulprits();
        }

        final IDebug debug = new RecipientProviderUtilities.IDebug() {
            @Override
            public void send(final String format, final Object... args) {
            }
        };

        final List<Run<?, ?>> builds = new ArrayList<Run<?, ?>>();
        Run<?, ?> build = run;
        builds.add(build);
        build = build.getPreviousCompletedBuild();

        while (build != null) {
            final Result buildResult = build.getResult();
            if (buildResult != null) {
                if (buildResult.isWorseThan(Result.SUCCESS)) {
                    builds.add(build);
                } else {
                    break;
                }
            }
            build = build.getPreviousCompletedBuild();
        }

        return RecipientProviderUtilities.getChangeSetAuthors(builds, debug);
    }

    // from Apache Commons Lang.... we code this little piece of code here
    // to minimize our dependencies...
    private static boolean isNullOrEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }
}
