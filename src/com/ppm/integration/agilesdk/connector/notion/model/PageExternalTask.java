package com.ppm.integration.agilesdk.connector.notion.model;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.notion.NotionConstants;
import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.ExternalTaskActuals;
import com.ppm.integration.agilesdk.provider.UserProvider;
import com.sun.jimi.core.util.P;

import java.util.*;

/**
 * Exposes a Notion Page object as an External Task, based on the passed config.
 */
public class PageExternalTask extends ExternalTask {

    private NotionPage page;
    private UserProvider userProvider;
    private ValueSet config;
    private Map<String, NotionPage.PropertyValue> propertiesByFieldId = new HashMap<>();
    private double percentComplete = 0.0d;
    private double actualEffort = 0.0d;
    private List<Long> resourcesIds = new ArrayList<>();

    public PageExternalTask(NotionPage page, ValueSet config, UserProvider userProvider) {
        this.page = page;
        this.config = config;
        this.userProvider = userProvider;
        this.page.properties.values().stream().forEach(propValue -> propertiesByFieldId.put(propValue.id, propValue));

        String resourceField = config.get(NotionConstants.KEY_TMF_TASK_RESOURCES);
        if (resourceField != null) {
            resourcesIds = getPeoplesField(resourceField);
        }

        String actualEffortField = config.get(NotionConstants.KEY_TMF_TASK_ACTUAL_EFFORT);
        if (actualEffortField != null) {
            Double effortValue = getNumberField(actualEffortField);
            if (effortValue != null) {
                actualEffort = effortValue.doubleValue();
            }
        }

        String percentCompleteField = config.get(NotionConstants.KEY_TMF_TASK_PERCENT_COMPLETE);
        if (percentCompleteField != null) {
            Double percentValue = getNumberField(percentCompleteField);
            if (percentValue != null) {
                percentComplete = percentValue * 100d; // PPM needs value between 0 and 100, while Notion stores percent in real value.

                if (percentComplete < 0) {
                    percentComplete = 0d;
                }
                if (percentComplete > 100d) {
                    percentComplete = 100d;
                }
            }
        }

        if (actualEffort > 0d && percentComplete <= 0d) {
            percentComplete = 1d;
        }

        if (actualEffort <= 0d && percentComplete > 0d) {
            actualEffort = 1d;
        }
    }

    @Override
    public TaskStatus getStatus() {
        // We compute task Status based on the percent complete value.
        if (percentComplete <= 0d) {
            return TaskStatus.READY;
        } else if (percentComplete < 100d) {
            return TaskStatus.IN_PROGRESS;
        } else {
            return TaskStatus.COMPLETED;
        }
    }

    @Override
    public String getId() {
        return page.id;
    }

    @Override
    public String getName() {
        String fieldId = config.get(NotionConstants.KEY_TMF_TASK_NAME);
        if (fieldId != null) {
            String name = getTextField(fieldId);
            if (name == null) {
                // It's common to have one empty line at the end of table in Notion Database
                name = "?";
            }
            return name;
        } else {
            return super.getName();
        }
    }

    @Override
    public Date getScheduledStart() {
        String fieldId = config.get(NotionConstants.KEY_TMF_TASK_START_DATE);
        if (fieldId != null) {
            Date date = getDateField(fieldId);
            if (date != null) {
                return adjustStartDateTime(date);
            }
        }
        return super.getScheduledStart();

    }

    @Override
    public Date getScheduledFinish() {
        String fieldId = config.get(NotionConstants.KEY_TMF_TASK_FINISH_DATE);
        if (fieldId != null) {
            Date date = getDateField(fieldId);
            if (date != null) {
                return adjustFinishDateTime(date);
            }
        }
        return super.getScheduledFinish();
    }

    @Override
    public List<ExternalTaskActuals> getActuals() {

        List<ExternalTaskActuals> actuals = new ArrayList<ExternalTaskActuals>();


        final double numResources = resourcesIds.size();

        if (resourcesIds.isEmpty()) {
            // All is unassigned effort
            ExternalTaskActuals unassignedActuals = new NotionExternalTaskActuals(actualEffort, percentComplete, getScheduledStart(), getScheduledFinish(), null);
            actuals.add(unassignedActuals);
        } else {
            // One Actual entry per resource.
            for (final Long resourceId : resourcesIds) {
                ExternalTaskActuals resourceActuals = new NotionExternalTaskActuals(actualEffort / numResources, percentComplete, getScheduledStart(), getScheduledFinish(), null);
                actuals.add(resourceActuals);
            }
        }

        return actuals;
    }

    private String getTextField(String fieldId) {
        NotionPage.PropertyValue prop = propertiesByFieldId.get(fieldId);

        if (prop == null) {
            return null;
        }

        return prop.getTextValue();
    }

    private Date getDateField(String fieldId) {

        boolean pickEndDate = false;

        if (fieldId != null && fieldId.endsWith(NotionConstants.END_DATE_SUFFIX)) {
            pickEndDate = true;
            fieldId = fieldId.substring(0, fieldId.length() - NotionConstants.END_DATE_SUFFIX.length());
        }

        NotionPage.PropertyValue prop = propertiesByFieldId.get(fieldId);

        if (prop == null) {
            return null;
        }

        return pickEndDate ? prop.getEndDateValue() : prop.getDateValue();
    }

    private List<Long> getPeoplesField(String fieldId) {
        NotionPage.PropertyValue prop = propertiesByFieldId.get(fieldId);

        if (prop == null) {
            return null;
        }

        return prop.getPeoplesValue(userProvider);
    }

    private Double getNumberField(String fieldId) {
        NotionPage.PropertyValue prop = propertiesByFieldId.get(fieldId);

        if (prop == null) {
            return null;
        }

        return prop.number;
    }
}
