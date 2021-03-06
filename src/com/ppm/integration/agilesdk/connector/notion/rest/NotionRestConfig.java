
/*
 * © Copyright 2019 - 2020 Micro Focus or one of its affiliates.
 */

package com.ppm.integration.agilesdk.connector.notion.rest;


import org.apache.wink.client.ClientConfig;

public class NotionRestConfig {
    private ClientConfig clientConfig;

    private String authToken;

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public NotionRestConfig() {
        clientConfig = new ClientConfig();
    }


    public ClientConfig setProxy(String proxyHost, String proxyPort) {

        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && !proxyPort.isEmpty()) {
            clientConfig.proxyHost(proxyHost);
            clientConfig.proxyPort(Integer.parseInt(proxyPort));
        }
        return clientConfig;
    }


    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

}
