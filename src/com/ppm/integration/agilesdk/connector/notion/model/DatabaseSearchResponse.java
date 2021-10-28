package com.ppm.integration.agilesdk.connector.notion.model;

public class DatabaseSearchResponse extends PaginatedResponse {

    public String object;
    public NotionDatabase[] results;

    @Override
    public NotionDatabase[] getResults() {
        return results;
    }
}
