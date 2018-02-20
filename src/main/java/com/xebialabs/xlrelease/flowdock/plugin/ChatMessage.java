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

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xebialabs.xlrelease.configuration.UserProfile;
import com.xebialabs.xlrelease.domain.Phase;
import com.xebialabs.xlrelease.domain.PlanItem;
import com.xebialabs.xlrelease.domain.Release;
import com.xebialabs.xlrelease.domain.Task;
import com.xebialabs.xlrelease.domain.Team;
import com.xebialabs.xlrelease.domain.status.TaskStatus;
import com.xebialabs.xlrelease.flowdock.plugin.exception.FlowdockException;
import com.xebialabs.xlrelease.service.UserProfileService;

/**
 * Created by ankurtrivedi on 02/05/16.
 */

@Service
public class ChatMessage extends FlowdockMessage{

    public static final String XLRELEASE_RELEASE_MAIL = "xlrelease@flowdock.com";

    protected String externalUserName;
    protected String subject;
    protected String fromAddress;
    protected String source;
    protected String event;
    protected UserProfileService UserProfileService;

    private static ChatMessage singleton;

    @Autowired
    public ChatMessage(UserProfileService UserProfileService) {
        this.externalUserName = "XLRelease";
        this.subject = "Message from XL Release";
        this.fromAddress = XLRELEASE_RELEASE_MAIL;
        this.source = "";
        this.UserProfileService = UserProfileService;
        register(this);
    }

    private static synchronized void register(ChatMessage chatMessage) {
        if (singleton == null) {
            singleton = chatMessage;
        }
    }

    public static ChatMessage getInstance() {
        return singleton;
    }

    public void setExternalUserName(String externalUserName) {

        this.externalUserName = externalUserName;
    }

    public void setSubject(String subject) {

        this.subject = subject;
    }

    public void setFromAddress(String fromAddress) {

        this.fromAddress = fromAddress;
    }

    public void setSource(String source) {

        this.source = source;
    }

    public void setEvent(String event) {

        this.event = event;
    }

    @Override
    public String asPostData() throws UnsupportedEncodingException {
        StringBuffer postData = new StringBuffer();
        postData.append("subject=").append(urlEncode(subject));
        postData.append("&content=").append(urlEncode(content));
        postData.append("&from_address=").append(urlEncode(fromAddress));
        postData.append("&source=").append(urlEncode(source));
        postData.append("&external_user_name=").append(urlEncode(externalUserName));
        postData.append("&tags=").append(urlEncode(tags));
        postData.append("&event=").append(urlEncode(event));
        return postData.toString();
    }

    public ChatMessage fromAuditableDeployitEvent(PlanItem pi) throws FlowdockException {
        String content = "";
        if (UserProfileService == null) {
            throw new FlowdockException("UserProfileService not initialized");
        }

        if(pi instanceof Release){
            Release release = (Release) pi;

            content = "@team Release " + pi.getProperty("title") + " has status " + ((Release) pi).getStatus().value();
        }
        else if (pi instanceof Phase){
            content = "@team Phase " + pi.getProperty("title") + " has status " + ((Phase) pi).getStatus().value();

        }
        else if(pi instanceof Task) {

            Task task = (Task) pi;

            UserProfile userProfile = UserProfileService.findByUsername(task.getReleaseOwner());

             if (!(pi.getProperty("owner") == null)) {
                 userProfile = UserProfileService.findByUsername(pi.getProperty("owner").toString());
             }



            if (task.getStatus().equals(TaskStatus.IN_PROGRESS)) {
                if(task.getTeam()== null || task.getTeam().isEmpty() ){
                    if(!userProfile.getFullName().isEmpty())
                                content = "@" +userProfile.getFullName()+" Approval Pending for " + pi.getProperty("title");
                    }

                    else{
                        Team currentTeam = task.getRelease().getAdminTeam() ;
                        String combinedTeamMessage = "";
                        List<Team> teams = task.getRelease().getTeams();
                            for(Team team: teams){
                                if (team.getTeamName().equalsIgnoreCase(task.getTeam())){
                                    currentTeam = team;
                                }
                            }

                            for (String member:currentTeam.getMembers()) {
                                userProfile = UserProfileService.findByUsername(member);
                                combinedTeamMessage = combinedTeamMessage + "@" + userProfile.getFullName() +" ";
                            }
                        content = combinedTeamMessage+" Approval Pending for " + pi.getProperty("title");
                        }



                //content = "@"+pi.getProperty("owner")+" Approval Pending for " + pi.getProperty("title");
            }
            else {

                content = "@team Task " + pi.getProperty("title") +
                        " assigned to " + userProfile.getFullName() + " has status " + ((Task) pi).getStatus().value();



            }

        }


        this.setContent(content);
        this.setSubject("XL Release event");
        this.setFromAddress(XLRELEASE_RELEASE_MAIL);
        this.setSource("XL Release");
        this.setTags("XL Release");
        this.setEvent("message");

        return this;
    }
}
