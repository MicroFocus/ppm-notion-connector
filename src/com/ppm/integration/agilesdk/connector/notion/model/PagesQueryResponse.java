package com.ppm.integration.agilesdk.connector.notion.model;

public class PagesQueryResponse extends PaginatedResponse {

    public String object;
    public NotionPage[] results;


    @Override
    public NotionPage[] getResults() {
        return results;
    }
}
