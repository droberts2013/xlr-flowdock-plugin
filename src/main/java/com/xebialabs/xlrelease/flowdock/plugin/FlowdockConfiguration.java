package com.xebialabs.xlrelease.flowdock.plugin;

/**
 * Created by jdewinne on 2/5/15.
 */
public class FlowdockConfiguration {

    private String apiUrl = "";
    private String flowToken = "";
    private Boolean enabled = Boolean.FALSE;

    public FlowdockConfiguration(String apiUrl, String flowToken, Boolean enabled) {
        this.apiUrl = apiUrl;
        this.flowToken = flowToken.replaceAll("\\s", "");
        this.enabled = enabled;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getFlowToken() {
        return flowToken;
    }

    public Boolean isEnabled() {
        return enabled;
    }
}
