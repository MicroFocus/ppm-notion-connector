package com.ppm.integration.agilesdk.connector.notion.model;

import com.hp.ppm.user.model.User;
import com.kintana.core.logging.LogManager;
import com.kintana.core.logging.Logger;
import com.ppm.integration.agilesdk.provider.UserProvider;
import org.apache.commons.lang.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class NotionPage extends NotionObject {

    private final static Logger logger = LogManager.getLogger(NotionPage.class);

    // yyyy-MM-dd date
    private final static SimpleDateFormat shortDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private final static DateTimeFormatter longDateTimeFormatter = new DateTimeFormatterBuilder()
            // date/time
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            // offset (hh:mm - "+00:00" when it's zero)
            .optionalStart().appendOffset("+HH:MM", "+00:00").optionalEnd()
            // offset (hhmm - "+0000" when it's zero)
            .optionalStart().appendOffset("+HHMM", "+0000").optionalEnd()
            // offset (hh - "Z" when it's zero)
            .optionalStart().appendOffset("+HH", "Z").optionalEnd()
            // create formatter
            .toFormatter();

    public Map<String, PropertyValue> properties;

    public class PropertyValue {
        public String id;
        public String name;
        public String type;
        public String email;
        public Option[] multi_select;
        public Option select;
        public Double number;
        public RichText[] rich_text;
        public RichText[] title;
        public People[] people;
        public DateRange date;
        public String created_time;
        public String last_edited_time;

        public class Format {
            public String format;
        }


        public class Option {
            public String id;
            public String name;
            public String color;
        }

        public class RichText {
            public String type;
            public String plain_text;
        }

        public class DateRange {
            public String start;
            public String end;
        }

        public class People {
            public String name;
            public String id;
            public Person person;

            public class Person {
                public String email;
            }

            public String getEmail() {
                if (person != null) {
                    return person.email;
                }

                return null;
            }
        }

        private String getRichText() {
            if (rich_text == null || rich_text.length == 0) {
                return "";
            }

            return Arrays.stream(rich_text).map(rt -> (rt.plain_text == null ? "" : rt.plain_text)).collect(Collectors.joining());
        }

        private String getTitleText() {
            if (title == null || title.length == 0) {
                return "";
            }

            return Arrays.stream(title).map(rt -> (rt.plain_text == null ? "" : rt.plain_text)).collect(Collectors.joining());
        }

        /** Return the text value of this property, taken from either title or rich_text depending on the type of property */
        public String getTextValue() {
            return "title".equals(type) ? getTitleText() : getRichText();
        }

        /** Returns the date value. Works for date/creation_time/last_update_time Properties.
         * If type is "date", returns the start date value.
         * @see #getEndDateValue() if you want the end date value. */
        public Date getDateValue() {
            String dateStr = null;
            if ("date".equals(type)) {
                dateStr = date != null ? date.start : null;
            } else if ("created_time".equals(type)) {
                dateStr = created_time;
            } else if ("last_edited_time".equals(type)) {
                dateStr = last_edited_time;
            }
            return parseDate(dateStr);
        }

        /** Only works if the field is of type "date".  Returns the end date. */
        public Date getEndDateValue() {
            String dateStr = null;
            if (date != null) {
                dateStr = date.end;
            }

            return parseDate(dateStr);
        }

        private Date parseDate(String dateStr) {
            if (StringUtils.isBlank(dateStr)) {
                return null;
            }

            try {
                if (dateStr.contains("T")) {
                    ZonedDateTime date = ZonedDateTime.parse(dateStr, longDateTimeFormatter);
                    return Date.from(date.toInstant());
                } else {
                    // Format yyyy-MM-dd
                    return shortDateFormat.parse(dateStr);
                }
            } catch (Exception e) {
                logger.error("Failed to parse Date string " + dateStr + " , ignoring date.", e);
                return null;
            }
        }

        /**
         * @return The list of the PPM User IDs based on the content of the emails or people for that property.
         *
         *
         */
        public List<Long> getPeoplesValue(UserProvider userProvider) {
            List<Long> ppmResourceIds = new ArrayList<>();
            if ("email".equals(type)) {
                if (StringUtils.isBlank(email)) {
                    // No User to match.
                    return ppmResourceIds;
                } else {
                    String[] emails = StringUtils.split(email, ";,");
                    for (String mailAddress: emails) {
                        Long userId = getResourceIdFromEmailOrUsername(mailAddress, userProvider);
                        if (userId == null) {
                            logger.error("We couldn't find a PPM USer with email address "+mailAddress.trim());
                        } else {
                            if (!ppmResourceIds.contains(userId)) {
                                ppmResourceIds.add(userId);
                            }
                        }
                    }
                }
            } else if ("people".equals(type)) {
                if (people != null) {
                    for (People p : people) {
                        Long userId = getResourceIdFromEmailOrUsername(p.getEmail(), userProvider);

                        if (userId == null) {
                            userId = getResourceIdFromEmailOrUsername(p.name, userProvider);
                        }

                        if (userId == null) {
                            logger.error("We couldn't find a PPM USer with email address or username "+p.getEmail() +" / "+p.name);
                        } else {
                            if (!ppmResourceIds.contains(userId)) {
                                ppmResourceIds.add(userId);
                            }
                        }
                    }
                }
            } else {
                logger.error("Cannot get People information from a field of type " + type);
            }
            return ppmResourceIds;
        }

        private Long getResourceIdFromEmailOrUsername(String emailOrUsername, UserProvider userProvider) {
            if (StringUtils.isBlank(emailOrUsername)) {
                return null;
            }
            User user = userProvider.getByEmail(emailOrUsername.trim());

            if (user == null) {
                user = userProvider.getByUsername(emailOrUsername.trim());
            }

            if (user == null) {
                return null;
            } else {
                return user.getUserId();
            }
        }
    }


}
