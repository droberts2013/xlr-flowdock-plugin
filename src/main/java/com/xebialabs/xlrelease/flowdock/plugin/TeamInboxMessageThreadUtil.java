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

import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xebialabs.xlrelease.api.XLReleaseServiceHolder;
import com.xebialabs.xlrelease.api.v1.ReleaseApi;
import com.xebialabs.xlrelease.domain.Release;
import com.xebialabs.xlrelease.domain.variables.StringVariable;
import com.xebialabs.xlrelease.domain.variables.Variable;
import com.xebialabs.xlrelease.flowdock.plugin.exception.FlowdockException;

public class TeamInboxMessageThreadUtil {    
	public static String TEAM_INBOX_MSG_ID = "teamInboxMsgId";
    Logger logger = LoggerFactory.getLogger(TeamInboxMessageThreadUtil.class);
    
    //NOTE - This method assumes only 1 flow defined for a release. 
    //If there is more than one, behavior is undefined, and this method must be updated to store a Map of message Ids within xlr release object..
    
    public String getTeamInboxMsgId(Release release, FlowdockApi api) throws InterruptedException {
    	ReleaseApi releaseApi = XLReleaseServiceHolder.getReleaseApi();
    	//see if value for release's teamInboxMsgId has already been set.
    	Map<String, Variable> varMap = release.getVariablesByKeys();
    	StringVariable relVar = (StringVariable)varMap.get(TEAM_INBOX_MSG_ID);

    	//If var for teamInboxMsgId is not defined, then release does not support threaded messages.
    	//TODO look into creating new release var via XLR REST api.
    	if (relVar == null) {
    		logger.debug("Release Not using threaded Flowdock messages: " + release.getName());
    		return null;
    	}

    	//var is defined, check for value
    	String teamInboxMsgId = relVar.getValue();
    	logger.debug("teamInboxMsgId = " + teamInboxMsgId );

    	if ("0".equals(teamInboxMsgId) || "".equals(teamInboxMsgId.trim())) {
    		//create new team inbox message for release and get its id.
    		teamInboxMsgId = createTeamInboxMsg(release, api);
    		relVar.setValue(teamInboxMsgId);
    		varMap.put(TEAM_INBOX_MSG_ID, relVar);

			try {
    			releaseApi.updateVariable(relVar.getId(), relVar);
    		} catch (Exception e) {
    			//always getting a timeout here after 1 min, but is working anyway. Compressing stack trace to simple debug message.
    			logger.debug("TeamInboxMsgThreadUtil: releaseApi.updateVariable() method - " + e.getMessage());
    		}
		}
    	return teamInboxMsgId;
    }
    
	private String createTeamInboxMsg(Release release, FlowdockApi api) {
		//value has not been set yet, create a new Team Inbox message for this release and get it's id.
    	logger.debug("Creating new team inbox message" );
		TeamInboxMessage teamInboxMsg;
		String teamInboxMsgId = null;
		try {
			teamInboxMsg = TeamInboxMessage.getInstance().fromAuditableDeployitEvent(release);
			String response = api.pushThreadedTeamInboxMessage(teamInboxMsg);
			
			teamInboxMsgId = getLongAsStringFromJson("id", response);
		} catch (FlowdockException e) {
			e.printStackTrace();
		}
    	logger.debug("New team inbox message created with id=" + teamInboxMsgId );
		return teamInboxMsgId;
	}
    
    private String getLongAsStringFromJson(String key, String json) {
    	logger.debug("getValueFromJson: key="+key + " json="+ json);
    	Long value = null;
    	try {
        	JSONParser parser = new JSONParser();
			Object obj = parser.parse(json);
			JSONObject jsonObject = (JSONObject) obj;
            value = (Long)jsonObject.get(key);
		} catch (ParseException e) {
			e.printStackTrace();
		}
    	return value.toString();
    }
    
    public static String convertReleaseIdToHyphens(String releaseIdWithSlashes) {
    	releaseIdWithSlashes = releaseIdWithSlashes.replace("/Release", "-Release");
    	releaseIdWithSlashes = releaseIdWithSlashes.replace("/Folder", "-Folder");
    	return releaseIdWithSlashes.replaceFirst("Applications-", "");
    }

}


