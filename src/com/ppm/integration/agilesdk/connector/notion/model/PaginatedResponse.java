package com.ppm.integration.agilesdk.connector.notion.model;

import java.util.List;

public abstract class PaginatedResponse {
    public boolean has_more;
    public String next_cursor;

    public abstract <T extends NotionObject> T[] getResults();
}
