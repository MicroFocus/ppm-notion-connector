package com.ppm.integration.agilesdk.connector.notion.model;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class NotionObject {

    public String object;
    public String created_time;
    public String id;
    public String last_edited_time;

    public Title[] title;

    public class Title {
        public String type;
        public String plain_text;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        if (title != null && title.length > 0) {
            String name = Arrays.stream(title).map(t -> (t.plain_text == null ? "" : t.plain_text)).collect(Collectors.joining());
            return name;
        }

        return "?";
    }
}

