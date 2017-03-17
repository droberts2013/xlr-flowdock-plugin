/**
 * Copyright 2017 XEBIALABS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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
