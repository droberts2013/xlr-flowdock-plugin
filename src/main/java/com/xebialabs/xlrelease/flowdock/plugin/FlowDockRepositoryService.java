package com.xebialabs.xlrelease.flowdock.plugin;

import com.xebialabs.deployit.plugin.api.reflect.Type;
import com.xebialabs.deployit.plugin.api.udm.ConfigurationItem;
import com.xebialabs.xlrelease.api.XLReleaseServiceHolder;
import com.xebialabs.deployit.repository.SearchParameters;
import com.xebialabs.xlrelease.flowdock.plugin.exception.FlowDockNotConfiguredException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jdewinne on 2/5/15.
 */
public class FlowDockRepositoryService {

    private List<FlowDockConfiguration> flowDockConfigurations;

    public List<FlowDockConfiguration> getFlowDockConfigurations() throws FlowDockNotConfiguredException {
        if (flowDockConfigurations == null) {
            setFlowDockConfigurations();
        }

        return flowDockConfigurations;
    }

    public Boolean isFlowDockEnabled() throws FlowDockNotConfiguredException {
        if (flowDockConfigurations == null) {
            setFlowDockConfigurations();
        }

        for (FlowDockConfiguration flowDockConfiguration : flowDockConfigurations) {
            if (flowDockConfiguration.isEnabled()) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    private void setFlowDockConfigurations() throws FlowDockNotConfiguredException {
        // Get flowdock properties
        SearchParameters parameters = new SearchParameters().setType(Type.valueOf("flowdock.configuration"));
        List<ConfigurationItem> query = XLReleaseServiceHolder.getRepositoryService().listEntities(parameters);
        if (query.size() > 0) {
            flowDockConfigurations = new ArrayList<FlowDockConfiguration>();
            for (ConfigurationItem read : query) {
                flowDockConfigurations.add(new FlowDockConfiguration((String) read.getProperty("apiUrl"), (String) read.getProperty("flowToken"), (Boolean) read.getProperty("enabled")));
            }
        } else {
            throw new FlowDockNotConfiguredException();
        }
    }


}
