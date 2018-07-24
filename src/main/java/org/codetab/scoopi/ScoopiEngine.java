package org.codetab.scoopi;

import javax.inject.Inject;

import org.codetab.scoopi.exception.CriticalException;
import org.codetab.scoopi.messages.Messages;
import org.codetab.scoopi.model.Log.CAT;
import org.codetab.scoopi.shared.StatService;
import org.codetab.scoopi.step.TaskMediator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScoopiEngine {

    static final Logger LOGGER = LoggerFactory.getLogger(ScoopiEngine.class);

    @Inject
    private ScoopiSystem scoopiSystem;
    @Inject
    private TaskMediator taskMediator;
    @Inject
    private StatService statService;

    /*
     * single thread env throws CriticalException and terminates the app and
     * multi thread env may also throw CriticalException but they terminates
     * just the executing thread
     *
     */
    public void start() {
        try {
            // single thread
            LOGGER.info(Messages.getString("ScoopiEngine.0")); //$NON-NLS-1$

            scoopiSystem.startStatService();

            scoopiSystem.addShutdownHook();

            String defaultConfigFile = "scoopi-default.xml"; //$NON-NLS-1$
            String userConfigFile = scoopiSystem.getPropertyFileName();
            scoopiSystem.initConfigService(defaultConfigFile, userConfigFile);

            scoopiSystem.startMetricsServer();

            scoopiSystem.initDefsProvider();

            scoopiSystem.initDataDefService();

            scoopiSystem.pushInitialPayload();

            LOGGER.info(Messages.getString("ScoopiEngine.1")); //$NON-NLS-1$

            scoopiSystem.waitForHeapDump();

            // multi thread

            LOGGER.info(Messages.getString("ScoopiEngine.2")); //$NON-NLS-1$

            taskMediator.start();

            taskMediator.waitForFinish();

            scoopiSystem.waitForHeapDump();

            LOGGER.info(Messages.getString("ScoopiEngine.3")); //$NON-NLS-1$
        } catch (CriticalException e) {
            LOGGER.error("{}", e.getMessage()); //$NON-NLS-1$
            LOGGER.warn(Messages.getString("ScoopiEngine.4")); //$NON-NLS-1$
            LOGGER.debug("{}", e); //$NON-NLS-1$
            statService.log(CAT.FATAL, e.getMessage(), e);
        } finally {
            scoopiSystem.stopMetricsServer();
            scoopiSystem.stopStatService();
            LOGGER.info(Messages.getString("ScoopiEngine.5")); //$NON-NLS-1$
        }
    }
}
