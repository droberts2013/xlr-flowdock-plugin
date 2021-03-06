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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xebialabs.xlrelease.configuration.UserProfile;
import com.xebialabs.xlrelease.domain.Phase;
import com.xebialabs.xlrelease.domain.PlanItem;
import com.xebialabs.xlrelease.domain.Release;
import com.xebialabs.xlrelease.domain.Task;
import com.xebialabs.xlrelease.flowdock.plugin.exception.FlowdockException;
import com.xebialabs.xlrelease.service.UserProfileService;

/**
 * Created by jdewinne on 2/5/15.
 */

@Service
public class TeamInboxMessage extends FlowdockMessage {

    public static final String XLRELEASE_RELEASE_MAIL = "xlrelease@flowdock.com";

    protected String externalUserName;
    protected String subject;
    protected String fromAddress;
    protected String source;
    protected UserProfileService UserProfileService;

    private static TeamInboxMessage singleton;

    @Autowired
    public TeamInboxMessage(UserProfileService UserProfileService) {
        this.externalUserName = "XLRelease";
        this.subject = "Message from XL Release";
        this.fromAddress = XLRELEASE_RELEASE_MAIL;
        this.source = "";
        this.UserProfileService = UserProfileService;
        register(this);
    }

    private static synchronized void register(TeamInboxMessage teamInboxMessage) {
        if (singleton == null) {
            singleton = teamInboxMessage;
        }
    }
    public static TeamInboxMessage getInstance() {
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

    @Override
    public String asPostData() throws UnsupportedEncodingException {
        StringBuffer postData = new StringBuffer();
        postData.append("subject=").append(urlEncode(subject));
        postData.append("&content=").append(urlEncode(content));
        postData.append("&from_address=").append(urlEncode(fromAddress));
        postData.append("&source=").append(urlEncode(source));
        postData.append("&external_user_name=").append(urlEncode(externalUserName));
        postData.append("&tags=").append(urlEncode(tags));
        return postData.toString();
    }

    public TeamInboxMessage fromAuditableDeployitEvent(PlanItem pi) throws FlowdockException {
        String content = "";
        if (UserProfileService == null) {
            throw new FlowdockException("UserProfileService not initialized");
        }


        if(pi instanceof Release){

            Release release = (Release) pi;

            UserProfile userProfile = UserProfileService.findByUsername(release.getOwner());

            if (!(pi.getProperty("owner") == null)) {
                userProfile = UserProfileService.findByUsername(pi.getProperty("owner").toString());
            }
            content = "Release " + pi.getProperty("title") +
                    " assigned to " + userProfile.getFullName() + " has status " + ((Release) pi).getStatus().value();

        }
        else if (pi instanceof Phase){
            content = "Phase " + pi.getProperty("title") + " has status " + ((Phase) pi).getStatus().value();

        }
        else if(pi instanceof Task){

            Task task = (Task) pi;

            UserProfile userProfile = UserProfileService.findByUsername(task.getReleaseOwner());

            if (!(pi.getProperty("owner") == null)) {
                userProfile = UserProfileService.findByUsername(pi.getProperty("owner").toString());
            }

            content = "Task " + pi.getProperty("title") +
                    " assigned to " + userProfile.getFullName() + " has status " + ((Task) pi).getStatus().value();

        }
        this.setContent(content);
        this.setSubject("XL Release event");
        this.setFromAddress(XLRELEASE_RELEASE_MAIL);
        this.setSource("XL Release");
        this.setTags("XL Release");

        return this;
    }
}
