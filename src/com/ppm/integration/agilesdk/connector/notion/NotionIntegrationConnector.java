
/*
 * © Copyright 2019 - 2020 Micro Focus or one of its affiliates.
 */

package com.ppm.integration.agilesdk.connector.notion;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.ppm.integration.agilesdk.FunctionIntegration;
import com.ppm.integration.agilesdk.IntegrationConnector;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.notion.model.NotionDatabase;
import com.ppm.integration.agilesdk.connector.notion.service.NotionServiceProvider;
import com.ppm.integration.agilesdk.model.AgileProject;
import com.ppm.integration.agilesdk.ui.*;
import org.apache.commons.lang.StringUtils;

/**
 * Main Connector class file for Jira Cloud connector.
 * Note that the Jira Cloud version is purely informative - there is no version for Jira Cloud.
 */
public class NotionIntegrationConnector extends IntegrationConnector {

    @Override
    public String getExternalApplicationName() {
        return "Notion";
    }

    @Override
    public String getExternalApplicationVersionIndication() {
        return "2021+";
    }

    @Override
    public String getConnectorVersion() {
        return "0.1";
    }

    @Override
    public String getTargetApplicationIcon() {
        return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAPCAYAAADtc08vAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsIAAA7CARUoSoAAAAIOSURBVDhPfZO9b7FRGMYvXxFFCYkKiVhUxGBoSAwGfwGLpEaDVUg6UIkYmQ0mf4JJLBIhErSpdDYZTK2PpOojqjjvc+6Xvq3X+/6SO3lOzrmvc5/rvh8RE8B/2O/3WK1W2Gw20Gq1EIvFhx3ihQSenp5QrVYxmUzw9vaG5XKJ7XaL3W4Hri+XyylRpVIhnU7Dbrcf8gW4QDgcZm63m7VaLTYcDpkgxGazGZvP50y4na3Xa4put8tCoRArlUpMqIinMikX+fz8xPPzM7LZLILBID4+PiAI/BX8KdPpFI1GA6PRCMlkEiTA4eV2Oh1YrVZcX1/DbDbD4XBAo9FQXF5e4uLiAjKZDLVaDY+Pj5T3wxFuUiKRQDQahdPphE6ng8/ng9frRb/fR6VSIVOvrq4OGScCRwQfEIvFEAgEkMvlKGmxWKBer8NgMBxO/easgM1mg9FopNILhQLa7TY9SalU0jO+c1aAI5VKcXd3B5FIhFQqhdfX18POT/4pwHG5XCTS6/VQLBbJ6FNIQCKR0OIc3NSbmxs8PDzQNJ5CAn6/n6btyPv7O/W82WxCoVAgn8/DYrGQJ6fQHEQiEQhTRv0VhotuymQy1HtetsfjoVHnay4+Ho/JG87Xz8RNur29hclkgl6vp7Ydg/8bPI7ffFLv7+8Rj8f/CHAGgwHK5TJ1gN+mVqu/pvB78HbyMwDwC5WVF0FawUbKAAAAAElFTkSuQmCC";
    }

    @Override
    public List<Field> getDriverConfigurationFields() {
        return Arrays.asList(new Field[]{
                new PlainText(NotionConstants.KEY_PROXY_HOST, "PROXY_HOST", "", false),
                new PlainText(NotionConstants.KEY_PROXY_PORT, "PROXY_PORT", "", false),
                new LineBreaker(),
                new LabelText("", "AUTHENTICATION_SETTINGS_SECTION", "block", false),
                new PasswordText(NotionConstants.KEY_INTEGRATION_TOKEN, "INTEGRATION_TOKEN", "", true),
                new CheckBox(NotionConstants.KEY_FORCE_INTEGRATION_TOKEN_USE, "LABEL_FORCE_INTEGRATION_TOKEN_USE", false)
        });
    }

    @Override
    public List<AgileProject> getAgileProjects(ValueSet instanceConfigurationParameters) {
        List<NotionDatabase> databases = NotionServiceProvider.get(instanceConfigurationParameters).getAllAvailableDatabases();

        return databases.stream().map(database -> {
            AgileProject proj = new AgileProject();
            proj.setValue(database.getId());
            proj.setDisplayName(database.getName());
            return proj;
        }).collect(Collectors.toList());

    }

    @Override
    public List<FunctionIntegration> getIntegrations() {
        return Arrays.asList(new FunctionIntegration[]{new NotionWorkPlanIntegration()});
    }

    @Override
    public List<String> getIntegrationClasses() {
        return Arrays.asList(new String[]{"com.ppm.integration.agilesdk.connector.notion.NotionWorkPlanIntegration"});
    }

}
