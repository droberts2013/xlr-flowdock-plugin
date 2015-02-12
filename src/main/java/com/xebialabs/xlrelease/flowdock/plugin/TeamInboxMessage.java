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
