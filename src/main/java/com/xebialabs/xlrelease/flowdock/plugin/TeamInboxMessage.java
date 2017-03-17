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

import com.xebialabs.deployit.plugin.api.udm.ConfigurationItem;

import java.io.UnsupportedEncodingException;

/**
 * Created by jdewinne on 2/5/15.
 */
public class TeamInboxMessage extends FlowdockMessage {

    public static final String XLRELEASE_RELEASE_STARTED_MAIL = "xlrelease+started@flowdock.com";

    protected String externalUserName;
    protected String subject;
    protected String fromAddress;
    protected String source;


    public TeamInboxMessage() {
        this.externalUserName = "XLRelease";
        this.subject = "Message from XL Release";
        this.fromAddress = XLRELEASE_RELEASE_STARTED_MAIL;
        this.source = "";
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

    public static TeamInboxMessage fromAuditableDeployitEvent(ConfigurationItem ci) {
        TeamInboxMessage msg = new TeamInboxMessage();
        StringBuffer content = new StringBuffer();
        content.append("XL Release event for ").append(ci.getId());
        content.append(" with message ").append(ci.getProperty("message"));
        content.append(" from user ").append(ci.getProperty("username"));

        msg.setContent(content.toString());
        msg.setSubject("XL Release event");
        msg.setFromAddress(XLRELEASE_RELEASE_STARTED_MAIL);
        msg.setSource("XL Release");
        msg.setTags("XL Release");

        return msg;
    }
}
