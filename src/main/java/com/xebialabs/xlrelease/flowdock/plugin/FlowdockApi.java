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


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;

import org.apache.jackrabbit.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.xebialabs.xlrelease.flowdock.plugin.exception.FlowdockException;

/**
 * Created by jdewinne on 2/5/15.
 */
public class FlowdockApi {

    Logger logger = LoggerFactory.getLogger(FlowdockApi.class);

    private FlowdockConfiguration flowdockConfiguration;

    public FlowdockApi(FlowdockConfiguration flowdockConfiguration) {
        this.flowdockConfiguration = flowdockConfiguration;
    }

    public void pushTeamInboxMessage(TeamInboxMessage msg) throws FlowdockException {
        try {
            logger.debug("FlowdockApi: Ready to send Team Inbox Notification");
            doPost(flowdockConfiguration.getApiUrl()+"/messages/team_inbox/"+flowdockConfiguration.getFlowToken(), msg.asPostData());
            logger.debug("FlowdockApi: Team Inbox Notification posted successfully");
        } catch(UnsupportedEncodingException ex) {
            throw new FlowdockException("Cannot encode request data: " + ex.getMessage());
        }
    }

    public void pushChatMessage(ChatMessage msg) throws FlowdockException {
        try {
            logger.debug("FlowdockApi: Ready to send chat Notification");
            doPost(flowdockConfiguration.getApiUrl()+"/flows/"+flowdockConfiguration.getOrgParamName()+"/messages", msg.asPostData());
            logger.debug("FlowdockApi: Chat Notification posted successfully");
        } catch(UnsupportedEncodingException ex) {
            throw new FlowdockException("Cannot encode request data: " + ex.getMessage());
        }
    }

    private void doPost(String flowdockUrl, String data) throws FlowdockException {
        URL url;
        HttpURLConnection connection = null;

        try {
            // create connection
            logger.debug("FlowdockApi: Creating connection");


            //connection with Proxy


            if (StringUtils.hasText(flowdockConfiguration.getProxyHost()) && flowdockConfiguration.getProxyPort() != 0) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(flowdockConfiguration.getProxyHost(), flowdockConfiguration.getProxyPort()));

                //  connection = (HttpURLConnection) url.openConnection();
                System.setProperty("https.proxyHost", flowdockConfiguration.getProxyHost());
                System.setProperty("https.proxyPort", String.valueOf(flowdockConfiguration.getProxyPort()));
                connection = (HttpURLConnection) new URL(flowdockUrl).openConnection(proxy);
            } else {
                url = new URL(flowdockUrl);
                connection = (HttpURLConnection) url.openConnection();
            }

            String encoded = Base64.encode(flowdockConfiguration.getApiKey());
            connection.setRequestProperty("Authorization", "Basic "+encoded);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Length", String.valueOf(data.getBytes().length));
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            // send the request

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(data);
            wr.flush();
            wr.close();

            if(connection.getResponseCode() != 200 && connection.getResponseCode() != 201 ) {
                StringBuffer responseContent = new StringBuffer();
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    String responseLine;
                    while ((responseLine = in.readLine()) != null) {
                        responseContent.append(responseLine);
                    }
                    in.close();

                } catch(Exception ex) {
                    logger.debug("FlowdockApi: Response is " + responseContent.toString());
                } finally {
                    throw new FlowdockException("Flowdock returned an error response with status " +
                            connection.getResponseCode() + " " + connection.getResponseMessage() + ", " +
                            responseContent.toString() + "\n\nURL: " + flowdockUrl);
                }
            }
        } catch(MalformedURLException ex) {
            throw new FlowdockException("Flowdock API URL is invalid: " + flowdockUrl);
        } catch(ProtocolException ex) {
            throw new FlowdockException("ProtocolException in connecting to Flowdock: " + ex.getMessage());
        } catch(IOException ex) {
            throw new FlowdockException("IOException in connecting to Flowdock: " + ex.getMessage());
        }
    }

    
    //**********************************************
    // Methods used for threaded flowdock messages
    //**********************************************
    
    public String pushThreadedTeamInboxMessage(TeamInboxMessage msg) throws FlowdockException {
    	String response;
        try {
            logger.debug("FlowdockApi: Ready to send Team Inbox Notification");
            String grcMsg = msg.asPostData() + "&event=" + msg.urlEncode("mail");
            logger.debug("TeamInboxMsg URL=" + grcMsg);
            
            response = doPostWithResponse(flowdockConfiguration.getApiUrl()+"/flows/"+flowdockConfiguration.getOrgParamName()+"/messages", grcMsg);
            logger.debug("FlowdockApi: Team Inbox Notification posted successfully");
            logger.debug("response=" + response);
        } catch(UnsupportedEncodingException ex) {
            throw new FlowdockException("Cannot encode request data: " + ex.getMessage());
        }
        return response;
    }


    public String pushThreadedChatMessage(ChatMessage msg, String teamInboxMessageId) throws FlowdockException {
    	String response;
        try {
            logger.debug("FlowdockApi: Ready to send chat Notification for thread TeamInboxMsgId="+teamInboxMessageId);
            msg.setEvent("comment");
            String postData = msg.asPostData();
            logger.debug("postData="+postData);
            response = doPostWithResponse(flowdockConfiguration.getApiUrl()+"/flows/"+flowdockConfiguration.getOrgParamName()+"/messages/" + teamInboxMessageId + "/comments", postData);
            logger.debug("FlowdockApi: Chat Notification posted successfully");
            logger.debug("response=" + response);
        } catch(UnsupportedEncodingException ex) {
            throw new FlowdockException("Cannot encode request data: " + ex.getMessage());
        }
        return response;
    }

    private String doPostWithResponse(String flowdockUrl, String data) throws FlowdockException {
        URL url;
        HttpURLConnection connection = null;
        StringBuilder response = null;

        try {
            // create connection
            logger.debug("FlowdockApi: Creating connection");


            //connection with Proxy

            if (!flowdockConfiguration.getProxyHost().isEmpty() && flowdockConfiguration.getProxyHost().trim().length() != 0
                    && flowdockConfiguration.getProxyPort() != 0) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(flowdockConfiguration.getProxyHost(), flowdockConfiguration.getProxyPort()));

                //  connection = (HttpURLConnection) url.openConnection();
                System.setProperty("https.proxyHost", flowdockConfiguration.getProxyHost());
                System.setProperty("https.proxyPort", String.valueOf(flowdockConfiguration.getProxyPort()));
                connection = (HttpURLConnection) new URL(flowdockUrl).openConnection(proxy);
            } else {
                url = new URL(flowdockUrl);
                connection = (HttpURLConnection) url.openConnection();
            }

            String encoded = Base64.encode(flowdockConfiguration.getApiKey());
            connection.setRequestProperty("Authorization", "Basic "+encoded);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Length", String.valueOf(data.getBytes().length));
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            
            // send the request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(data);
            wr.flush();
            wr.close();

            if(connection.getResponseCode() != 200 && connection.getResponseCode() != 201 ) {
                StringBuffer responseContent = new StringBuffer();
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    String responseLine;
                    while ((responseLine = in.readLine()) != null) {
                        responseContent.append(responseLine);
                    }
                    in.close();

                } catch(Exception ex) {
                    logger.debug("FlowdockApi: Response is " + responseContent.toString());
                } finally {
                    throw new FlowdockException("Flowdock returned an error response with status " +
                            connection.getResponseCode() + " " + connection.getResponseMessage() + ", " +
                            responseContent.toString() + "\n\nURL: " + flowdockUrl);
                }
            } else {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                	response.append(inputLine);
                }
                in.close();

            }
        } catch(MalformedURLException ex) {
            throw new FlowdockException("Flowdock API URL is invalid: " + flowdockUrl);
        } catch(ProtocolException ex) {
            throw new FlowdockException("ProtocolException in connecting to Flowdock: " + ex.getMessage());
        } catch(IOException ex) {
            throw new FlowdockException("IOException in connecting to Flowdock: " + ex.getMessage());
        }
        
        return response.toString();
    }
    
    
}
