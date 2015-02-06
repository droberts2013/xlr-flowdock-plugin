package com.xebialabs.xlrelease.flowdock.plugin;


import com.xebialabs.xlrelease.flowdock.plugin.exception.FlowDockException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.net.ProtocolException;
import java.net.MalformedURLException;

/**
 * Created by jdewinne on 2/5/15.
 */
public class FlowDockApi {
    private FlowDockConfiguration flowDockConfiguration;

    public FlowDockApi(FlowDockConfiguration flowDockConfiguration) {
        this.flowDockConfiguration = flowDockConfiguration;
    }

    public void pushTeamInboxMessage(TeamInboxMessage msg) throws FlowDockException {
        try {
            doPost("/messages/team_inbox/", msg.asPostData());
        } catch(UnsupportedEncodingException ex) {
            throw new FlowDockException("Cannot encode request data: " + ex.getMessage());
        }
    }

    private void doPost(String path, String data) throws FlowDockException {
        URL url;
        HttpURLConnection connection = null;
        String flowdockUrl = flowDockConfiguration.getApiUrl() + path + flowDockConfiguration.getFlowToken();
        try {
            // create connection
            url = new URL(flowdockUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", String.valueOf(data.getBytes().length));
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            // send the request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(data);
            wr.flush();
            wr.close();

            if(connection.getResponseCode() != 200) {
                StringBuffer responseContent = new StringBuffer();
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    String responseLine;
                    while ((responseLine = in.readLine()) != null) {
                        responseContent.append(responseLine);
                    }
                    in.close();
                } catch(Exception ex) {
                    // nothing we can do about this
                } finally {
                    throw new FlowDockException("Flowdock returned an error response with status " +
                            connection.getResponseCode() + " " + connection.getResponseMessage() + ", " +
                            responseContent.toString() + "\n\nURL: " + flowdockUrl);
                }
            }
        } catch(MalformedURLException ex) {
            throw new FlowDockException("Flowdock API URL is invalid: " + flowdockUrl);
        } catch(ProtocolException ex) {
            throw new FlowDockException("ProtocolException in connecting to Flowdock: " + ex.getMessage());
        } catch(IOException ex) {
            throw new FlowDockException("IOException in connecting to Flowdock: " + ex.getMessage());
        }
    }

}
