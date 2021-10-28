/*
 * © Copyright 2019 - 2020 Micro Focus or one of its affiliates.
 */

package com.ppm.integration.agilesdk.connector.notion.service;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.notion.NotionConstants;
import com.ppm.integration.agilesdk.connector.notion.NotionIntegrationConnector;
import com.ppm.integration.agilesdk.connector.notion.rest.NotionRestClient;
import com.ppm.integration.agilesdk.connector.notion.rest.NotionRestConfig;
import com.ppm.integration.agilesdk.provider.Providers;
import com.ppm.integration.agilesdk.provider.UserProvider;
import org.apache.commons.lang.StringUtils;

public class NotionServiceProvider {

    public static UserProvider getUserProvider() {
        return Providers.getUserProvider(NotionIntegrationConnector.class);
    }

    public static NotionService get(ValueSet config) {

        String proxyHost = config.get(NotionConstants.KEY_PROXY_HOST);

        NotionRestConfig restConfig = new NotionRestConfig();

        if (!StringUtils.isBlank(proxyHost)) {
            String proxyPort = config.get(NotionConstants.KEY_PROXY_PORT);
            if (StringUtils.isBlank(proxyPort)) {
                proxyPort = "80";
            }

            restConfig.setProxy(proxyHost, proxyPort);
        }
        restConfig.setAuthToken(getIntegrationToken(config));

        return new NotionService(new NotionRestClient(restConfig));
    }

    public static String getIntegrationToken(ValueSet config) {
        String integrationToken = config.get(NotionConstants.KEY_USER_INTEGRATION_TOKEN);
        if (!StringUtils.isBlank(integrationToken)) {
            return integrationToken;
        } else {
            return config.get(NotionConstants.KEY_INTEGRATION_TOKEN);
        }
    }

}
