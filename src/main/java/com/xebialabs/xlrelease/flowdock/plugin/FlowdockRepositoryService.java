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

import java.util.ArrayList;
import java.util.List;

import com.xebialabs.deployit.plugin.api.reflect.Type;
import com.xebialabs.deployit.plugin.api.udm.ConfigurationItem;
import com.xebialabs.deployit.repository.SearchParameters;
import com.xebialabs.deployit.repository.RepositoryServiceHolder;
import com.xebialabs.xlrelease.flowdock.plugin.exception.FlowdockNotConfiguredException;

/**
 * Created by jdewinne on 2/5/15.
 */
public class FlowdockRepositoryService {

    private List<FlowdockConfiguration> flowdockConfigurations;

    public List<FlowdockConfiguration> getFlowdockConfigurations() throws FlowdockNotConfiguredException {
        if (flowdockConfigurations == null) {
            setFlowdockConfigurations();
        }

        return flowdockConfigurations;
    }

    public Boolean isFlowdockEnabled() throws FlowdockNotConfiguredException {
        if (flowdockConfigurations == null) {
            setFlowdockConfigurations();
        }

        for (FlowdockConfiguration flowdockConfiguration : flowdockConfigurations) {
            if (flowdockConfiguration.isEnabled()) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    private void setFlowdockConfigurations() throws FlowdockNotConfiguredException {
        // Get flowdock properties
        SearchParameters parameters = new SearchParameters().setType(Type.valueOf("flowdock.configuration"));
        List<ConfigurationItem> query = RepositoryServiceHolder.getRepositoryService().listEntities(parameters);
        if (query.size() > 0) {
            flowdockConfigurations = new ArrayList<FlowdockConfiguration>();
            for (ConfigurationItem read : query) {
               flowdockConfigurations.add(new FlowdockConfiguration((String) read.getProperty("apiUrl"), (String) read.getProperty("apiKey"), (Boolean) read.getProperty("enabled"),(String) read.getProperty("proxyHost"),(Integer) read.getProperty("proxyPort"),(String) read.getProperty("templateName"),(String) read.getProperty("flowToken"),(String) read.getProperty("orgParamName")));
//                flowdockConfigurations.add(new FlowdockConfiguration((String) read.getProperty("apiUrl"), (String) read.getProperty("apiKey"), (Boolean) read.getProperty("enabled"),"",0,(String) read.getProperty("templateName"),(String) read.getProperty("flowToken"),(String) read.getProperty("orgParamName")));
            }
        } else {
            throw new FlowdockNotConfiguredException();
        }
    }


}
