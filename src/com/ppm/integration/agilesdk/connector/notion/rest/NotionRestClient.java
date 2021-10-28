
/*
 * © Copyright 2019 - 2020 Micro Focus or one of its affiliates.
 */

package com.ppm.integration.agilesdk.connector.notion.rest;


import com.kintana.core.logging.LogLevel;
import com.kintana.core.logging.LogManager;
import com.kintana.core.logging.Logger;
import com.ppm.integration.agilesdk.connector.notion.NotionConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;

import javax.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.UUID;

public class NotionRestClient {

    private boolean ENABLE_REST_CALLS_STATUS_LOG = true;

    private final static Logger logger = LogManager.getLogger(NotionRestClient.class);

    private RestClient restClient;
    private NotionRestConfig notionConfig;
    private ClientConfig clientConfig;

    public NotionRestClient(NotionRestConfig notionConfig) {
        this.notionConfig = notionConfig;
        this.clientConfig = notionConfig.getClientConfig();
        this.restClient = new RestClient(clientConfig);
    }

    /**

     * @param includeContentTypeHeader if true, we'll include the JSon "Content-Type" header. If false, we'll not include any Content-type header (to use when using GET or DELETE).
     * @return
     */
    private Resource getNotionResource(String fullUrl, boolean includeContentTypeHeader, String uuid) {
        Resource resource;
        try {
            URL url = new URL(fullUrl);
            String urlPath = url.getHost();
            if (url.getPort() > 0) {
                urlPath = urlPath + ":" + url.getPort();
            }
            URI uri = null;
            try {
                uri = new URI(url.getProtocol(), urlPath, url.getPath(), url.getQuery() == null ? null : URLDecoder.decode(url.getQuery(), "UTF-8"), null);
            } catch (UnsupportedEncodingException e) {
                // This will never happen.
                throw new RuntimeException("Impossible encoding error occurred", e);
            }
            resource = restClient.resource(uri).accept(MediaType.APPLICATION_JSON).header("Authorization", "Bearer "+ notionConfig.getAuthToken());

            // Following header is required for easy HTTP request tracing in systems such as DataPower.
            if (uuid != null) {
                resource.header("X-B3-TraceId", uuid);
            }

            if (includeContentTypeHeader) {
                resource.contentType(MediaType.APPLICATION_JSON);
            }

            // All notion API calls should include Notion-Version Header.
            resource.header("Notion-Version", NotionConstants.NOTION_API_VERSION);

        } catch (MalformedURLException e) {
            throw new RestRequestException( // is a malformed URL
                    400, String.format("%s is a malformed URL", fullUrl));
        } catch (URISyntaxException e) {
            throw new RestRequestException(400, String.format("%s is a malformed URL", fullUrl));
        }
        return resource;
    }

    public ClientResponse sendGet(String uri) {

        if (ENABLE_REST_CALLS_STATUS_LOG) {
            logger.log(LogLevel.STATUS, "GET "+uri);
        }

        String uuid = UUID.randomUUID().toString();
        Resource resource = this.getNotionResource(uri, false, uuid);
        ClientResponse response = resource.get();

        checkResponseStatus(200, response, uri, "GET", null, uuid);

        return response;
    }

    private void checkResponseStatus(int expectedHttpStatusCode, ClientResponse response, String uri, String verb, String payload, String uuid) {

        if (response.getStatusCode() != expectedHttpStatusCode) {
            StringBuilder errorMessage = new StringBuilder(String.format("## Unexpected HTTP response status code %s for %s uri %s, expected %s", response.getStatusCode(), verb,  uri, expectedHttpStatusCode));
            if (uuid != null) {
                errorMessage.append(System.lineSeparator()).append("Value of HTTP tracking header X-B3-TraceId:").append(uuid);
            }
            if (payload != null) {
                errorMessage.append(System.lineSeparator()).append(System.lineSeparator()).append("# Sent Payload:").append(System.lineSeparator()).append(payload);
            }
            String responseStr = null;
            try {
                responseStr = response.getEntity(String.class);
            } catch (Exception e) {
                // we don't do anything if we cannot get the response.
            }
            if (!StringUtils.isBlank(responseStr)) {
                errorMessage.append(System.lineSeparator()).append(System.lineSeparator()).append("# Received Response:").append(System.lineSeparator()).append(responseStr);
            }

            throw new RestRequestException(response.getStatusCode(), errorMessage.toString());
        }

    }

    public ClientResponse sendPost(String uri, String jsonPayload, int expectedHttpStatusCode) {

        if (ENABLE_REST_CALLS_STATUS_LOG) {
            logger.log(LogLevel.STATUS, "POST "+uri);
        }

        String uuid = UUID.randomUUID().toString();
        Resource resource = this.getNotionResource(uri, true, uuid);
        ClientResponse response = resource.post(jsonPayload);
        checkResponseStatus(expectedHttpStatusCode, response, uri, "POST", jsonPayload, uuid);

        return response;
    }

    public ClientResponse sendPut(String uri, String jsonPayload, int expectedHttpStatusCode) {

        if (ENABLE_REST_CALLS_STATUS_LOG) {
            logger.log(LogLevel.STATUS, "PUT "+uri);
        }

        String uuid = UUID.randomUUID().toString();
        Resource resource = this.getNotionResource(uri,true, uuid);
        ClientResponse response = resource.put(jsonPayload);

        checkResponseStatus(expectedHttpStatusCode, response, uri, "PUT", jsonPayload, uuid);

        return response;
    }

    public String getIntegrationToken() {
        return notionConfig.getAuthToken();
    }

    public NotionRestConfig getNotionRestConfig() {
        return notionConfig;
    }
}
