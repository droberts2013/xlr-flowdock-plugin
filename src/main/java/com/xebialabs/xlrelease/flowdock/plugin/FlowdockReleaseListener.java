/**
 * Copyright 2018 XEBIALABS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.xebialabs.xlrelease.flowdock.plugin;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xebialabs.deployit.engine.spi.event.DeployitEventListener;
import com.xebialabs.xlrelease.api.XLReleaseServiceHolder;
import com.xebialabs.xlrelease.domain.Release;
import com.xebialabs.xlrelease.domain.events.ReleaseExecutedEvent;
import com.xebialabs.xlrelease.domain.status.ReleaseStatus;
import com.xebialabs.xlrelease.flowdock.plugin.exception.FlowdockException;
import com.xebialabs.xlrelease.flowdock.plugin.exception.FlowdockNotConfiguredException;

import nl.javadude.t2bus.Subscribe;

/**
 * Created by ankurtrivedi on 02/05/16.
 */
@DeployitEventListener
public class FlowdockReleaseListener {
    Logger logger = LoggerFactory.getLogger(FlowdockReleaseListener.class);

    public FlowdockReleaseListener() {
    }

    @Subscribe
    public void receiveReleaseEvent(ReleaseExecutedEvent event) {

        try {
        	String eventReleaseId = event.release().getId();
            logger.debug("eventReleaseId: " + eventReleaseId);
            
        	Release release = event.release();
            ReleaseStatus releaseStatus = release.getStatus();
        	
            if (release.getOriginTemplateId() == null) {
            	try {
            		String idWithHyphens = TeamInboxMessageThreadUtil.convertReleaseIdToHyphens(release.getId());
                	release = XLReleaseServiceHolder.getReleaseApi().getRelease(idWithHyphens);
            	} catch (Throwable e) {
            		logger.error("Flowdock: exception getting release using id with hyphens: " + TeamInboxMessageThreadUtil.convertReleaseIdToHyphens(release.getId()));
            	}
            }
        	if (release == null || release.getOriginTemplateId() == null) {
        		try {
            		release = XLReleaseServiceHolder.getReleaseApi().getRelease(event.release().getId());
        		} catch (Throwable e) {
            		logger.error("Flowdock: exception getting release using id: " + event.release().getId());
        		}
        	}                            
            
            //Get title of template
            if (release.getOriginTemplateId() == null) {
            	logger.error("Flowdock: CANNOT DETERMINE RELEASE TEMPLATE - TEMPLATE ID IS NULL!!!");
            	return;
            } 
            
            FlowdockRepositoryService flowdockRepositoryService = new FlowdockRepositoryService();

            String originTemplateId = release.getOriginTemplateId();
            logger.debug("ORIGIN TEMPLATE ID: " + originTemplateId);

            String templateName = XLReleaseServiceHolder.getTemplateApi().getTemplate(originTemplateId).getTitle();

            // Get flowdock properties
            List<FlowdockConfiguration> flowdockConfigurations = flowdockRepositoryService.getFlowdockConfigurations();

            for (FlowdockConfiguration flowdockConfiguration : flowdockConfigurations) {
                if (flowdockConfiguration.isEnabled() && flowdockConfiguration.getTemplateName().equalsIgnoreCase(templateName)) {

            		FlowdockApi api = new FlowdockApi(flowdockConfiguration);
            		String teamInboxMsgId = new TeamInboxMessageThreadUtil().getTeamInboxMsgId(release, api);

            		if (teamInboxMsgId != null) {
            			//using threaded flowdock messages
            			logger.debug("Flowdock: Sending threaded messages using teamInboxMsgId=" + teamInboxMsgId);
            			TeamInboxMessage msg = TeamInboxMessage.getInstance().fromAuditableDeployitEvent(release);
            			ChatMessage chatMessage = ChatMessage.getInstance().fromAuditableDeployitEvent(release);

            			if (releaseStatus.hasBeenStarted() && releaseStatus.isActive()) {
            				if (releaseStatus.equals(ReleaseStatus.FAILED)) {
            					api.pushThreadedChatMessage(chatMessage, teamInboxMsgId);
            				} else if (releaseStatus.equals(ReleaseStatus.COMPLETED)) {
            					api.pushThreadedChatMessage(chatMessage, teamInboxMsgId);
            				} else if (releaseStatus.equals(ReleaseStatus.ABORTED)) {
            					api.pushThreadedChatMessage(chatMessage, teamInboxMsgId);
            				}
            			}
            		} else {
            			// Send message to flowdock
            			logger.debug("FlowdockReleaseListener: Ready to send Team Inbox Notification");
            			TeamInboxMessage msg = TeamInboxMessage.getInstance().fromAuditableDeployitEvent(release);
            			ChatMessage chatMessage = ChatMessage.getInstance().fromAuditableDeployitEvent(release);

            			if (releaseStatus.hasBeenStarted() && releaseStatus.isActive()) {
            				if (releaseStatus.equals(ReleaseStatus.FAILED)) {
            					api.pushTeamInboxMessage(msg);
            					api.pushChatMessage(chatMessage);

            				} else if (releaseStatus.equals(ReleaseStatus.COMPLETED)) {
            					api.pushTeamInboxMessage(msg);

            				} else if (releaseStatus.equals(ReleaseStatus.ABORTED)) {
            					api.pushTeamInboxMessage(msg);
            				}
            			}
            		}
                }
            }
        } catch (FlowdockNotConfiguredException e) {
            // Do nothing, as Flowdock is not yet configured.
        } catch (FlowdockException e) {
            logger.error("Flowdock exception", e);
        } catch (Throwable t) {
            logger.error("Unexpected exception", t);
        }
    }
}
