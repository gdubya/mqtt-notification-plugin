package jenkins.plugins.mqttnotification;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

/**
 * A {@link RunListener} that sends MQTT notifications when a build starts.
 */
@Extension
public class MqttRunListener extends RunListener<Run<?, ?>> {

    @Override
    public void onStarted(final Run<?, ?> run, final TaskListener listener) {
        if (run.getParent() instanceof AbstractProject) {
            final AbstractProject<?, ?> project = (AbstractProject<?, ?>) run.getParent();
            final MqttNotifier notifier = project.getPublishersList().get(MqttNotifier.class);
            if (notifier != null && notifier.isNotifyOnStart()) {
                notifier.sendNotification(
                    notifier.getEffectiveStartTopic(),
                    notifier.getEffectiveStartMessage(),
                    run,
                    listener
                );
            }
        }
    }
}
