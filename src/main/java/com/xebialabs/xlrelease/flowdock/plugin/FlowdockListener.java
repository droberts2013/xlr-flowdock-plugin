package com.xebialabs.xlrelease.flowdock.plugin;

import com.xebialabs.deployit.engine.spi.event.AuditableDeployitEvent;
import com.xebialabs.deployit.engine.spi.event.CiBaseEvent;
import com.xebialabs.deployit.engine.spi.event.DeployitEventListener;

import com.xebialabs.deployit.plugin.api.reflect.Type;
import com.xebialabs.deployit.plugin.api.udm.ConfigurationItem;
import com.xebialabs.xlrelease.flowdock.plugin.exception.FlowdockException;
import com.xebialabs.xlrelease.flowdock.plugin.exception.FlowdockNotConfiguredException;
import nl.javadude.t2bus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * Created by jdewinne on 2/4/15.
 */
@DeployitEventListener
public class FlowdockListener {

    Logger logger = LoggerFactory.getLogger(FlowdockListener.class);

    public FlowdockListener() {
    }

    @Subscribe
    public void sendUpdateToFlowdock(AuditableDeployitEvent event) {
        FlowdockRepositoryService flowdockRepositoryService = new FlowdockRepositoryService();
        try {
            if (flowdockRepositoryService.isFlowdockEnabled()) {
                if (event instanceof CiBaseEvent) {
                    CiBaseEvent ciEvent = (CiBaseEvent) event;
                    for (ConfigurationItem ci : ciEvent.getCis()) {
                        if (ci.getType().equals(Type.valueOf("xlrelease.ActivityLogEntry"))) {
                            // Get flowdock properties
                            List<FlowdockConfiguration> flowdockConfigurations = flowdockRepositoryService.getFlowdockConfigurations();

                            for (FlowdockConfiguration flowdockConfiguration : flowdockConfigurations) {
                                if (flowdockConfiguration.isEnabled()) {
                                    // Send message to flowdock
                                    FlowdockApi api = new FlowdockApi(flowdockConfiguration);
                                    TeamInboxMessage msg = TeamInboxMessage.fromAuditableDeployitEvent(ci);
                                    api.pushTeamInboxMessage(msg);
                                    logger.info("Flowdock: Team Inbox notification sent successfully");
                                }
                            }
                        }
                    }
                }
            }
        } catch (FlowdockNotConfiguredException e) {
            // Do nothing, as Flowdock is not yet configured.
        } catch (FlowdockException e) {
            e.printStackTrace();
        }

    }
}
