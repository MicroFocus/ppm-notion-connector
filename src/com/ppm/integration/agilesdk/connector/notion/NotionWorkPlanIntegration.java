
/*
 * © Copyright 2019 - 2020 Micro Focus or one of its affiliates.
 */

package com.ppm.integration.agilesdk.connector.notion;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.notion.model.NotionDatabase;
import com.ppm.integration.agilesdk.connector.notion.model.NotionPage;
import com.ppm.integration.agilesdk.connector.notion.model.PageExternalTask;
import com.ppm.integration.agilesdk.connector.notion.service.NotionService;
import com.ppm.integration.agilesdk.connector.notion.service.NotionServiceProvider;
import com.ppm.integration.agilesdk.pm.*;
import com.ppm.integration.agilesdk.provider.LocalizationProvider;
import com.ppm.integration.agilesdk.provider.Providers;
import com.ppm.integration.agilesdk.provider.UserProvider;
import com.ppm.integration.agilesdk.ui.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class NotionWorkPlanIntegration extends WorkPlanIntegration {


    private final Logger logger = Logger.getLogger(NotionWorkPlanIntegration.class);

    public NotionWorkPlanIntegration() {
    }

    private NotionService service;

    private synchronized NotionService getService(ValueSet config) {
        if (service == null) {
            service = NotionServiceProvider.get(config);
        } else {
            service.refreshRestConfigIfNeeded(config);
        }
        return service;
    }

    @Override
    public List<Field> getMappingConfigurationFields(WorkPlanIntegrationContext context, ValueSet values) {

        final LocalizationProvider lp = Providers.getLocalizationProvider(NotionIntegrationConnector.class);

        List<Field> fields = new ArrayList<>();

        boolean isIntegrationTokenEmpty = StringUtils.isBlank(values.get(NotionConstants.KEY_INTEGRATION_TOKEN));

        if (isIntegrationTokenEmpty || !"true".equals(values.get(NotionConstants.KEY_FORCE_INTEGRATION_TOKEN_USE))) {
            // Users can use their own integration token
            fields.addAll(getUserIntegrationTokenFields(isIntegrationTokenEmpty));
        }

        // We only retrieve all databases and include the DB select field if the value of selected database is not already provided
        if (StringUtils.isBlank(values.get(NotionConstants.KEY_WP_DATABASE))) {

            DynamicDropdown databasesList = new DynamicDropdown(NotionConstants.KEY_WP_DATABASE, "WP_DATABASE", true) {
                @Override
                public List<String> getDependencies() {
                    return Arrays.asList(new String[]{NotionConstants.KEY_INTEGRATION_TOKEN, NotionConstants.KEY_USER_INTEGRATION_TOKEN});
                }

                @Override
                public List<Option> getDynamicalOptions(ValueSet values) {
                    final List<NotionDatabase> dbs = getService(values).getAllAvailableDatabases();
                    Collections.sort(dbs, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
                    List<Option> options = new ArrayList<>();
                    dbs.stream().forEach(db -> options.add(new DynamicDropdown.Option(db.getId(), db.getName())));
                    return options;
                }
            };

            fields.add(new LabelText("LABEL_DATABASE_TO_SYNC", "LABEL_DATABASE_TO_SYNC",
                    "Select what database to import:", true));


            fields.add(databasesList);

            fields.add(new LineBreaker());
        }

        // Fields mapping section
        fields.addAll(getTaskFieldMappingSectionFields());

        // Status mapping
        fields.addAll(getSortSectionFields());

        return fields;
    }

    private List<Field> getUserIntegrationTokenFields(boolean isIntegrationTokenEmpty) {
        List<Field> userTokenFields = new ArrayList<>(3);

        userTokenFields.add(new LabelText("LABEL_USER_INTEGRATION_TOKEN", "LABEL_USER_INTEGRATION_TOKEN",
                "Integration Token", false));

        if (isIntegrationTokenEmpty) {
            // Mandatory!
            userTokenFields.add(new PasswordText(NotionConstants.KEY_USER_INTEGRATION_TOKEN, "LABEL_USER_INTEGRATION_MANDATORY",
                    "", true));
        } else {
            // Optional
            userTokenFields.add(new PasswordText(NotionConstants.KEY_USER_INTEGRATION_TOKEN, "LABEL_USER_INTEGRATION_OPTIONAL",
                    "", false));
        }

        userTokenFields.add(new LineBreaker());

        return userTokenFields;
    }

    /**
     * @return the UI fields where users can map a property of the Notion database to each PPM Task field.
     * <p>
     * We will only propose the fields that have a compatible type for matching.
     */
    private List<Field> getTaskFieldMappingSectionFields() {

        List<Field> taskFields = new ArrayList<>(10);

        taskFields.add(new LabelText("LABEL_FIELDS_MAPPING", "LABEL_FIELDS_MAPPING",
                "Pick Properties to use for Task fields", false));

        taskFields.add(createTaskField(NotionConstants.KEY_TMF_TASK_NAME, "LABEL_TMF_TASK_NAME", true, "title", "rich_text"));
        taskFields.add(createTaskField(NotionConstants.KEY_TMF_TASK_START_DATE, "LABEL_TMF_TASK_START_DATE", false, "date", "created_time"));
        taskFields.add(createTaskField(NotionConstants.KEY_TMF_TASK_FINISH_DATE, "LABEL_TMF_TASK_FINISH_DATE", false, "date", "created_time"));
        taskFields.add(createTaskField(NotionConstants.KEY_TMF_TASK_RESOURCES, "LABEL_TMF_TASK_RESOURCES", false, "people", "email"));
        taskFields.add(createTaskField(NotionConstants.KEY_TMF_TASK_PERCENT_COMPLETE, "LABEL_TMF_TASK_PERCENT_COMPLETE", false, "percent"));
        taskFields.add(createTaskField(NotionConstants.KEY_TMF_TASK_ACTUAL_EFFORT, "LABEL_TMF_TASK_ACTUAL_EFFORT", false, "number"));

        taskFields.add(new LineBreaker());

        return taskFields;

    }

    private Field createTaskField(String fieldKey, String labelKey, boolean isRequired, String... supportedNotionFieldTypes) {
        Field f = new DynamicDropdown(fieldKey, labelKey, isRequired) {

            @Override
            public List<String> getDependencies() {
                return Arrays.asList(new String[]{NotionConstants.KEY_WP_DATABASE});
            }

            @Override
            public List<Option> getDynamicalOptions(ValueSet values) {
                String selectedDB = values.get(NotionConstants.KEY_WP_DATABASE);
                NotionDatabase db = getService(values).getNotionDatabase(selectedDB);

                if (db == null || db.properties == null) {
                    return new ArrayList<>();
                }

                List<Option> options = new ArrayList<>();

                Set<String> supportedTypes = new HashSet<>(Arrays.asList(supportedNotionFieldTypes));

                db.properties.values().stream().forEach(prop -> {
                    if (supportedTypes.contains("*") || supportedTypes.contains(prop.type) || supportedTypes.contains(prop.getNumberFormat())) {
                        options.add(new Option(prop.id, prop.name));
                        if ("date".equals(prop.type)) {
                            // Adding an extra field option to pick end date of a date field
                            options.add(new Option(prop.id + NotionConstants.END_DATE_SUFFIX, prop.name + " (end date)"));
                        }
                    }
                });

                db.properties.values().stream().filter(prop -> Arrays.stream(supportedNotionFieldTypes).anyMatch(supportedType -> (supportedType.equals(prop.type) || supportedType.equals(prop.getNumberFormat())))).map(prop -> new Option(prop.id, prop.name)).collect(Collectors.toList());

                return options;
            }
        };

        return f;

    }

    private List<Field> getSortSectionFields() {

        List<Field> sortFields = new ArrayList<>(3);

        sortFields.add(new LabelText("LABEL_SORTING_TITLE", "LABEL_SORTING_TITLE",
                "Tasks Sorting", false));

        sortFields.add(createTaskField(NotionConstants.KEY_WP_SORT_BY, "LABEL_SORT_BY", false, "*"));

        sortFields.add(new LineBreaker());

        return sortFields;
    }


    @Override
    /**
     * This method is in Charge of retrieving all Notion DB rows and turning them into a workplan structure to be imported in PPM.
     */
    public ExternalWorkPlan getExternalWorkPlan(WorkPlanIntegrationContext context, final ValueSet values) {

        final String dbId = values.get(NotionConstants.KEY_WP_DATABASE);

        final String sortByFieldId = values.get(NotionConstants.KEY_WP_SORT_BY);
        final List<NotionPage> rows = getService(values).getAllPages(dbId, sortByFieldId);

        final UserProvider userProvider = NotionServiceProvider.getUserProvider();

        return new ExternalWorkPlan() {

            @Override
            public List<ExternalTask> getRootTasks() {

                List<ExternalTask> rootTasks = rows.stream().map(page -> new PageExternalTask(page, values, userProvider)).collect(Collectors.toList());

                return rootTasks;
            }
        };

    }

    /**
     * This will allow to have the information in PPM DB table PPMIC_WORKPLAN_MAPPINGS of what entity in JIRA is effectively linked to the PPM work plan task.
     * It is very useful for reporting purpose.
     *
     * @since 9.42
     */
    public LinkedTaskAgileEntityInfo getAgileEntityInfoFromMappingConfiguration(ValueSet values) {
        LinkedTaskAgileEntityInfo info = new LinkedTaskAgileEntityInfo();

        String dbId = values.get(NotionConstants.KEY_WP_DATABASE);

        info.setProjectId(dbId);

        // THere's no specific info in Notion - no Epic or feature or such.

        return info;
    }


    @Override
    public boolean supportTimesheetingAgainstExternalWorkPlan() {
        return true;
    }
}
