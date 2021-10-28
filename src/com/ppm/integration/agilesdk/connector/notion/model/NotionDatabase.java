package com.ppm.integration.agilesdk.connector.notion.model;

import java.util.Map;

public class NotionDatabase extends NotionObject {

    public Map<String, Property> properties;

    public class Property {
        public String id;
        public String name;
        public String type;
        public Options multi_select;
        public Options select;
        public Format number;

        public class Format {
            public String format;
        }

        public class Options {
            Option[] options;

            public class Option {
                public String id;
                public String name;
                public String color;
            }
        }

        public String getNumberFormat() {
            if (number != null && number.format != null) {
                return number.format;
            }

            return "";
        }
    }
}

