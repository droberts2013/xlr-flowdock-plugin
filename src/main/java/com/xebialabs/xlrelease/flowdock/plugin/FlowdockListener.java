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

import com.xebialabs.deployit.engine.spi.event.AuditableDeployitEvent;
import com.xebialabs.deployit.engine.spi.event.CiBaseEvent;
import com.xebialabs.deployit.engine.spi.event.DeployitEventListener;
import com.xebialabs.deployit.plugin.api.reflect.Type;
import com.xebialabs.deployit.plugin.api.udm.ConfigurationItem;
import com.xebialabs.xlrelease.api.XLReleaseServiceHolder;
import com.xebialabs.xlrelease.domain.Phase;
import com.xebialabs.xlrelease.domain.Release;
import com.xebialabs.xlrelease.domain.Task;
import com.xebialabs.xlrelease.domain.status.PhaseStatus;
import com.xebialabs.xlrelease.domain.status.ReleaseStatus;
import com.xebialabs.xlrelease.domain.status.TaskStatus;
import com.xebialabs.xlrelease.flowdock.plugin.exception.FlowdockException;
import com.xebialabs.xlrelease.flowdock.plugin.exception.FlowdockNotConfiguredException;

import nl.javadude.t2bus.Subscribe;


/**
 * Created by jdewinne on 2/4/15.
 */
@DeployitEventListener()
public class FlowdockListener {

    Logger logger = LoggerFactory.getLogger(FlowdockListener.class);

    public FlowdockListener() {
    }

    @Subscribe
    public void sendUpdateToFlowdock(AuditableDeployitEvent event) {
        try {
            FlowdockRepositoryService flowdockRepositoryService = new FlowdockRepositoryService();
            logger.debug("Flowdock: Listener triggered Successfully");
            logger.debug("Flowdock: event.getClass() :"+ event.getClass());
            if (flowdockRepositoryService.isFlowdockEnabled()) {
                if (event instanceof CiBaseEvent) {
                    CiBaseEvent ciEvent = (CiBaseEvent) event;

                    for (ConfigurationItem ci : ciEvent.getCis()) {
                        logger.debug("Flowdock: Event type is "+ ci.getType());

                        if (ci.getType().equals(Type.valueOf("xlrelease.Release"))) {
                            logger.debug("Flowdock: Found an Release log entry");
                            Release releaseObject = (Release) ci;
                            logger.debug("Flowdock: Release ID is " + releaseObject.getId());
                            logger.debug("Release Id: " + releaseObject.getId());

                            Release release = releaseObject;
                            ReleaseStatus releaseStatus = release.getStatus();

                            if (release.getOriginTemplateId() == null) {
                            	try {
                            		String idWithHyphens = TeamInboxMessageThreadUtil.convertReleaseIdToHyphens(releaseObject.getId());
                                	release = XLReleaseServiceHolder.getReleaseApi().getRelease(idWithHyphens);
                            	} catch (Throwable e) {
                            		logger.error("Flowdock: exception getting release using id with hyphens: " + TeamInboxMessageThreadUtil.convertReleaseIdToHyphens(releaseObject.getId()));
                            	}
                            }
                        	if (release == null || release.getOriginTemplateId() == null) {
                        		try {
                            		release = XLReleaseServiceHolder.getReleaseApi().getRelease(releaseObject.getId());
                        		} catch (Throwable e) {
                            		logger.error("Flowdock: exception getting release using id: " + releaseObject.getId());
                        		}
                        	}                            
                            
                            //Get title of template
                            if (release.getOriginTemplateId() == null) {
                            	logger.error("Flowdock: CANNOT DETERMINE RELEASE TEMPLATE - TEMPLATE ID IS NULL!!!");
                            	continue;
                            } 
                            
                            logger.debug("Flowdock: looking for origin template: " + release.getOriginTemplateId());
                            Release template = XLReleaseServiceHolder.getTemplateApi().getTemplate(release.getOriginTemplateId());
                            String templateTitle = template.getTitle();
                            logger.debug("Flowdock: Template title = " + templateTitle);

                            // Get flowdock properties
                            List<FlowdockConfiguration> flowdockConfigurations = flowdockRepositoryService.getFlowdockConfigurations();

                            for (FlowdockConfiguration flowdockConfiguration : flowdockConfigurations) {
                            	if (flowdockConfiguration.isEnabled() && flowdockConfiguration.getTemplateName().equalsIgnoreCase((templateTitle))) {

                            		FlowdockApi api = new FlowdockApi(flowdockConfiguration);
                            		String teamInboxMsgId = new TeamInboxMessageThreadUtil().getTeamInboxMsgId(release, api);

                            		if (teamInboxMsgId != null) {
                            			//using threaded flowdock messages
                            			logger.debug("Flowdock: Sending threaded messages using teamInboxMsgId=" + teamInboxMsgId);
                            			TeamInboxMessage msg = TeamInboxMessage.getInstance().fromAuditableDeployitEvent(release);
                            			ChatMessage chatMessage = ChatMessage.getInstance().fromAuditableDeployitEvent(release);

                            			if (release.hasBeenStarted()) {
                            				if (releaseStatus.hasBeenStarted() && releaseStatus.isActive()) {
                            					if (releaseStatus.equals(ReleaseStatus.FAILED)) {
                            						api.pushThreadedChatMessage(chatMessage, teamInboxMsgId);
                            					} else if (releaseStatus.equals(ReleaseStatus.COMPLETED)) {
                            						api.pushThreadedChatMessage(chatMessage, teamInboxMsgId);
                            					}
                            				} else if(releaseStatus.equals(ReleaseStatus.ABORTED)) {
                            					api.pushThreadedChatMessage(chatMessage, teamInboxMsgId);
                            				}
                            			}
                            			logger.info("Flowdock: Release Team Inbox notification sent successfully");

                            		} else { 
                            			// message threads not supported:
                            			logger.debug("Flowdock: Ready to send Release Team Inbox Notification - release var teamInboxMsgId not found");
                            			TeamInboxMessage msg = TeamInboxMessage.getInstance().fromAuditableDeployitEvent(release);
                            			ChatMessage chatMessage = ChatMessage.getInstance().fromAuditableDeployitEvent(release);

                            			if (releaseStatus.hasBeenStarted() && releaseStatus.isActive()) {
                            				if (releaseStatus.equals(ReleaseStatus.FAILED)) {

                            					api.pushTeamInboxMessage(msg);
                            					api.pushChatMessage(chatMessage);

                            				} else if (releaseStatus.equals(ReleaseStatus.COMPLETED)) {
                            					api.pushTeamInboxMessage(msg);

                            				}
                            			} else if(releaseStatus.equals(ReleaseStatus.ABORTED)) {
                            				api.pushTeamInboxMessage(msg);
                            				api.pushChatMessage(chatMessage);
                            			}
                            			logger.info("Flowdock: Release Team Inbox notification sent successfully");
                            		}
                            	}
                            }
                        } else if (ci.getType().equals(Type.valueOf("xlrelease.Phase"))) {

                            logger.debug("Flowdock: Found a Phase entry");
                            Phase phaseObject = (Phase) ci;
                            logger.debug("Flowdock: Phase entry is " + phaseObject.getId());
                            logger.debug("ORIGIN TEMPLATEID: " + phaseObject.getRelease().getOriginTemplateId());
                            
                            //Get the release from the release name
                            Phase phase = XLReleaseServiceHolder.getPhaseApi().getPhase(phaseObject.getId());

                            PhaseStatus phaseStatus = phase.getStatus();

                            String templateTitle = XLReleaseServiceHolder.getTemplateApi().getTemplate(phaseObject.getRelease().getOriginTemplateId()).getTitle();

                            // Get flowdock properties
                            List<FlowdockConfiguration> flowdockConfigurations = flowdockRepositoryService.getFlowdockConfigurations();

                            for (FlowdockConfiguration flowdockConfiguration : flowdockConfigurations) {
                                if (flowdockConfiguration.isEnabled() && flowdockConfiguration.getTemplateName().equalsIgnoreCase((templateTitle))) {

                            		FlowdockApi api = new FlowdockApi(flowdockConfiguration);
                            		String teamInboxMsgId = new TeamInboxMessageThreadUtil().getTeamInboxMsgId(phase.getRelease(), api);

                            		if (teamInboxMsgId != null) {
                            			//using threaded flowdock messages
                            			logger.debug("Flowdock: Sending threaded messages using teamInboxMsgId=" + teamInboxMsgId);
                            			TeamInboxMessage msg = TeamInboxMessage.getInstance().fromAuditableDeployitEvent(phase);
                            			ChatMessage chatMessage = ChatMessage.getInstance().fromAuditableDeployitEvent(phase);

                            			if (phaseStatus.hasBeenStarted() && phaseStatus.isActive()) {
                            				if (phaseStatus.equals(PhaseStatus.FAILED)) {
                            					api.pushThreadedChatMessage(chatMessage, teamInboxMsgId);
                            				}
                            				else if(phaseStatus.equals(PhaseStatus.PLANNED)){
                            					api.pushThreadedChatMessage(chatMessage, teamInboxMsgId);
                            				}
                            				else if (phaseStatus.equals(PhaseStatus.COMPLETED)) {
                            					api.pushThreadedChatMessage(chatMessage, teamInboxMsgId);
                            				} else if (phaseStatus.equals(PhaseStatus.ABORTED)) {
                            					api.pushThreadedChatMessage(chatMessage, teamInboxMsgId);
                            				}
                            			}
                            		} else { 

                            			// Send message to flowdock
                            			logger.debug("Flowdock: Ready to send Phase Team Inbox Notification");
                            			TeamInboxMessage msg = TeamInboxMessage.getInstance().fromAuditableDeployitEvent(phase);
                            			ChatMessage chatMessage = ChatMessage.getInstance().fromAuditableDeployitEvent(phase);

                            			if (phaseStatus.hasBeenStarted() && phaseStatus.isActive()) {
                            				if (phaseStatus.equals(PhaseStatus.FAILED)) {
                            					//api.pushTeamInboxMessage(msg);
                            					api.pushChatMessage(chatMessage);
                            				}
                            				else if(phaseStatus.equals(PhaseStatus.PLANNED)){
                            					api.pushTeamInboxMessage(msg);
                            				}
                            				else if (phaseStatus.equals(PhaseStatus.COMPLETED)) {
                            					api.pushTeamInboxMessage(msg);

                            				} else if (phaseStatus.equals(PhaseStatus.ABORTED)) {
                            					api.pushTeamInboxMessage(msg);
                            				}
                            			}
                            		}
                                }
                            }
                        } else if (ci.getType().equals(Type.valueOf("xlrelease.Task"))||ci.getType().equals(Type.valueOf("xlrelease.CustomScriptTask"))) {

                            logger.debug("Flowdock: Found a Task entry");
                            Task taskObject = (Task) ci;
                            logger.debug("Flowdock: Task entry is " + taskObject.getId());

                            Release release = taskObject.getRelease();
                            
                            logger.debug("Task Block: Release Id: " + release.getId());
                            
                            String originTemplateId = release.getOriginTemplateId();
                            logger.debug("Origin Template ID: " + originTemplateId);

                            //Get the release from the release name
                            Task task = XLReleaseServiceHolder.getTaskApi().getTask(taskObject.getId());

                            TaskStatus taskStatus = task.getStatus();

                            logger.debug("Task Status is: "+ taskStatus.toString());

                            //Get title of template

                            String templateName = XLReleaseServiceHolder.getTemplateApi().getTemplate(originTemplateId).getName();
                            logger.debug("Template Name: " + templateName);

                            // Get flowdock properties
                            List<FlowdockConfiguration> flowdockConfigurations = flowdockRepositoryService.getFlowdockConfigurations();

                            for (FlowdockConfiguration flowdockConfiguration : flowdockConfigurations) {
                                if (flowdockConfiguration.isEnabled() && flowdockConfiguration.getTemplateName().equalsIgnoreCase((templateName))) {

                            		FlowdockApi api = new FlowdockApi(flowdockConfiguration);
                            		String teamInboxMsgId = new TeamInboxMessageThreadUtil().getTeamInboxMsgId(task.getRelease(), api);

                            		if (teamInboxMsgId != null) {
                            			//using threaded flowdock messages
                            			logger.debug("Flowdock: Sending threaded messages using teamInboxMsgId=" + teamInboxMsgId);
                            			TeamInboxMessage msg = TeamInboxMessage.getInstance().fromAuditableDeployitEvent(task);
                            			ChatMessage chatMessage = ChatMessage.getInstance().fromAuditableDeployitEvent(task);

                            			if (taskStatus.hasBeenStarted() && taskStatus.isActive()) {
                            				if (taskStatus.equals(TaskStatus.FAILED)) {
                            					api.pushThreadedChatMessage(chatMessage, teamInboxMsgId);
                            				} else if (taskStatus.equals(TaskStatus.IN_PROGRESS)){
                            					if (ci.getType().equals(Type.valueOf("xlrelease.Task"))){
                            						api.pushThreadedChatMessage(chatMessage, teamInboxMsgId);
                            					}
                            				}
                            				else if (taskStatus.equals(TaskStatus.COMPLETED)) {
                            					api.pushThreadedChatMessage(chatMessage, teamInboxMsgId);
                            				} else if (taskStatus.equals(TaskStatus.ABORTED)) {
                            					api.pushThreadedChatMessage(chatMessage, teamInboxMsgId);
                            				}

                            			}
                            		} else {
                            			// Send message to flowdock
                            			logger.debug("Flowdock: Ready to send Task Team Inbox Notification");
                            			TeamInboxMessage msg = TeamInboxMessage.getInstance().fromAuditableDeployitEvent(task);
                            			ChatMessage chatMessage = ChatMessage.getInstance().fromAuditableDeployitEvent(task);

                            			if (taskStatus.hasBeenStarted() && taskStatus.isActive()) {
                            				if (taskStatus.equals(TaskStatus.FAILED)) {
                            					api.pushTeamInboxMessage(msg);
                            					api.pushChatMessage(chatMessage);
                            				} else if (taskStatus.equals(TaskStatus.IN_PROGRESS)){
                            					if (ci.getType().equals(Type.valueOf("xlrelease.Task"))){
                            						api.pushChatMessage(chatMessage);    
                            					}
                            				}
                            				else if (taskStatus.equals(TaskStatus.COMPLETED)) {
                            					api.pushChatMessage(chatMessage);
                            					api.pushTeamInboxMessage(msg);
                            				} else if (taskStatus.equals(TaskStatus.ABORTED)) {
                            					api.pushChatMessage(chatMessage);
                            				}
                            			}
                            		}
	                            }
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
            logger.error("Unhandled exception", t);
        }
    }
}
