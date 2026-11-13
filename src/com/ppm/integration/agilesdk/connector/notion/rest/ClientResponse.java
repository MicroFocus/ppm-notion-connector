package com.ppm.integration.agilesdk.connector.notion.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight response model kept intentionally close to Wink ClientResponse
 * to minimize changes in existing connector code.
 */
public class ClientResponse {

    private int statusCode;
    private String message;
    private Object entity;
    private final Map<String, List<String>> headers = new HashMap<String, List<String>>();
    private final Map<String, Object> attributes = new HashMap<String, Object>();

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public <T> T getEntity(Class<T> clazz) {
        if (entity == null) {
            return null;
        }
        if (clazz == String.class) {
            return clazz.cast(String.valueOf(entity));
        }
        if (clazz.isInstance(entity)) {
            return clazz.cast(entity);
        }
        throw new IllegalArgumentException("Unsupported entity class: " + clazz.getName());
    }

    public void setEntity(Object entity) {
        this.entity = entity;
    }

    public void consumeContent() {
        // No-op for in-memory entity.
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public <T> void setAttribute(Class<T> key, T value) {
        attributes.put(key.getName(), value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(Class<T> key) {
        return (T) attributes.get(key.getName());
    }
}

