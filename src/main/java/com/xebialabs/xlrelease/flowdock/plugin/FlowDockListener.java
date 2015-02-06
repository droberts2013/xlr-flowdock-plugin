package com.xebialabs.xlrelease.flowdock.plugin;

import com.xebialabs.deployit.engine.spi.event.AuditableDeployitEvent;
import com.xebialabs.deployit.engine.spi.event.CiBaseEvent;
import com.xebialabs.deployit.engine.spi.event.DeployitEventListener;

import com.xebialabs.deployit.plugin.api.reflect.Type;
import com.xebialabs.deployit.plugin.api.udm.ConfigurationItem;
import com.xebialabs.xlrelease.flowdock.plugin.exception.FlowDockException;
import com.xebialabs.xlrelease.flowdock.plugin.exception.FlowDockNotConfiguredException;
import nl.javadude.t2bus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * Created by jdewinne on 2/4/15.
 */
@DeployitEventListener
public class FlowDockListener {

    Logger logger = LoggerFactory.getLogger(FlowDockListener.class);

    public FlowDockListener() {
    }

    @Subscribe
    public void sendUpdateToFlowDock(AuditableDeployitEvent event) {
        FlowDockRepositoryService flowDockRepositoryService = new FlowDockRepositoryService();
        try {
            if (flowDockRepositoryService.isFlowDockEnabled()) {
                if (event instanceof CiBaseEvent) {
                    CiBaseEvent ciEvent = (CiBaseEvent) event;
                    for (ConfigurationItem ci : ciEvent.getCis()) {
                        if (ci.getType().equals(Type.valueOf("xlrelease.ActivityLogEntry"))) {
                            // Get flowdock properties
                            List<FlowDockConfiguration> flowDockConfigurations = flowDockRepositoryService.getFlowDockConfigurations();

                            for (FlowDockConfiguration flowDockConfiguration : flowDockConfigurations) {
                                if (flowDockConfiguration.isEnabled()) {
                                    // Send message to flowdock
                                    FlowDockApi api = new FlowDockApi(flowDockConfiguration);
                                    TeamInboxMessage msg = TeamInboxMessage.fromAuditableDeployitEvent(ci);
                                    api.pushTeamInboxMessage(msg);
                                    logger.info("Flowdock: Team Inbox notification sent successfully");
                                }
                            }
                        }
                    }
                }
            }
        } catch (FlowDockNotConfiguredException e) {
            // Do nothing, as Flowdock is not yet configured.
        } catch (FlowDockException e) {
            e.printStackTrace();
        }

    }
}
