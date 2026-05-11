/*
 * © Copyright 2019 - 2020 Micro Focus or one of its affiliates.
 */

package com.ppm.integration.agilesdk.connector.notion.rest;


import com.kintana.core.logging.LogLevel;
import com.kintana.core.logging.LogManager;
import com.kintana.core.logging.Logger;
import com.ppm.integration.agilesdk.connector.notion.NotionConstants;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.UUID;

public class NotionRestClient {

    private boolean ENABLE_REST_CALLS_STATUS_LOG = true;

    private final static Logger logger = LogManager.getLogger(NotionRestClient.class);

    private RestTemplate restTemplate;
    private NotionRestConfig notionConfig;

    public NotionRestClient(NotionRestConfig notionConfig) {
        this.notionConfig = notionConfig;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        if (!StringUtils.isBlank(notionConfig.getProxyHost()) && notionConfig.getProxyPort() > 0) {
            requestFactory.setProxy(new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(notionConfig.getProxyHost(), notionConfig.getProxyPort())));
        }
        if (notionConfig.getConnectTimeout() > 0) {
            requestFactory.setConnectTimeout(notionConfig.getConnectTimeout());
        }
        if (notionConfig.getReadTimeout() > 0) {
            requestFactory.setReadTimeout(notionConfig.getReadTimeout());
        }

        this.restTemplate = new RestTemplate(requestFactory);
        this.restTemplate.setErrorHandler(new ResponseErrorHandler() {
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
            public void handleError(ClientHttpResponse response) throws IOException {
                // Errors are handled by checkResponseStatus to preserve existing behavior.
            }
        });
    }

    private URI getNotionUri(String fullUrl) {
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
            return uri;
        } catch (MalformedURLException e) {
            throw new RestRequestException( // is a malformed URL
                    400, String.format("%s is a malformed URL", fullUrl));
        } catch (URISyntaxException e) {
            throw new RestRequestException(400, String.format("%s is a malformed URL", fullUrl));
        }
    }

    private HttpEntity<String> getNotionRequestEntity(boolean includeContentTypeHeader, String uuid, String payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + notionConfig.getAuthToken());
        headers.set(HttpHeaders.ACCEPT, org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
        headers.set("Notion-Version", NotionConstants.NOTION_API_VERSION);

        if (uuid != null) {
            headers.set("X-B3-TraceId", uuid);
        }
        if (includeContentTypeHeader) {
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        }
        return new HttpEntity<String>(payload, headers);
    }

    private ClientResponse toClientResponse(final ResponseEntity<String> responseEntity) {
        ClientResponse response = new ClientResponse();
        response.setStatusCode(responseEntity.getStatusCodeValue());
        response.setMessage(responseEntity.getStatusCode().getReasonPhrase());
        response.setEntity(responseEntity.getBody());
        response.getHeaders().putAll(responseEntity.getHeaders());
        return response;
    }

    public ClientResponse sendGet(String uri) {

        if (ENABLE_REST_CALLS_STATUS_LOG) {
            logger.log(LogLevel.STATUS, "GET "+uri);
        }

        String uuid = UUID.randomUUID().toString();
        URI notionUri = this.getNotionUri(uri);
        HttpEntity<String> requestEntity = this.getNotionRequestEntity(false, uuid, null);
        ResponseEntity<String> springResponse;
        try {
            springResponse = restTemplate.exchange(notionUri, HttpMethod.GET, requestEntity, String.class);
        } catch (RestClientException e) {
            throw new RestRequestException(500, "Unexpected REST client error for GET uri " + uri + ": " + e.getMessage());
        }
        ClientResponse response = toClientResponse(springResponse);

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
        URI notionUri = this.getNotionUri(uri);
        HttpEntity<String> requestEntity = this.getNotionRequestEntity(true, uuid, jsonPayload);
        ResponseEntity<String> springResponse;
        try {
            springResponse = restTemplate.exchange(notionUri, HttpMethod.POST, requestEntity, String.class);
        } catch (RestClientException e) {
            throw new RestRequestException(500, "Unexpected REST client error for POST uri " + uri + ": " + e.getMessage());
        }
        ClientResponse response = toClientResponse(springResponse);
        checkResponseStatus(expectedHttpStatusCode, response, uri, "POST", jsonPayload, uuid);

        return response;
    }

    public ClientResponse sendPut(String uri, String jsonPayload, int expectedHttpStatusCode) {

        if (ENABLE_REST_CALLS_STATUS_LOG) {
            logger.log(LogLevel.STATUS, "PUT "+uri);
        }

        String uuid = UUID.randomUUID().toString();
        URI notionUri = this.getNotionUri(uri);
        HttpEntity<String> requestEntity = this.getNotionRequestEntity(true, uuid, jsonPayload);
        ResponseEntity<String> springResponse;
        try {
            springResponse = restTemplate.exchange(notionUri, HttpMethod.PUT, requestEntity, String.class);
        } catch (RestClientException e) {
            throw new RestRequestException(500, "Unexpected REST client error for PUT uri " + uri + ": " + e.getMessage());
        }
        ClientResponse response = toClientResponse(springResponse);

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
