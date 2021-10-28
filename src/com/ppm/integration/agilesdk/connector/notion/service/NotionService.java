package com.ppm.integration.agilesdk.connector.notion.service;

import com.google.gson.*;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.notion.NotionConstants;
import com.ppm.integration.agilesdk.connector.notion.model.*;
import com.ppm.integration.agilesdk.connector.notion.rest.NotionRestClient;
import com.ppm.integration.agilesdk.connector.notion.rest.NotionRestConfig;
import okhttp3.OkHttpClient;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientResponse;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class in charge of making calls to Notion REST API when needed. Contains a cache, so the service should not be a static member of a class, as the caches are never invalidated and might contain stale data if used as such.
 *
 * This class not thread safe.
 */
public class NotionService {

    private final static Logger logger = Logger.getLogger(NotionService.class);

    private NotionRestClient restClient;
    public NotionService(NotionRestClient restClient) {
        this.restClient = restClient;
    }

    private List<NotionDatabase> allAvailableDatabases = null;

    private Map<String, NotionDatabase> dbById = new HashMap<>();

    public List<NotionDatabase> getAllAvailableDatabases() {

        if (allAvailableDatabases == null) {

            String searchDatabasesPayload = "{\"filter\": {\"value\": \"database\", \"property\": \"object\"}, \"page_size\": 100}";

            List<NotionDatabase> results = runPaginatedPost(NotionConstants.API_SEARCH, searchDatabasesPayload, DatabaseSearchResponse.class, NotionDatabase.class);

            allAvailableDatabases = results;

            allAvailableDatabases.stream().forEach(db -> {dbById.put(db.id, db);});
        }

        return allAvailableDatabases;
    }

    public NotionDatabase getNotionDatabase(String dbId) {

        if (!dbById.containsKey(dbId)) {

            ClientResponse response = restClient.sendGet(NotionConstants.API_GET_SINGLE_DB + dbId);

            NotionDatabase db = new Gson().fromJson(response.getEntity(String.class), NotionDatabase.class);

            dbById.put(db.id, db);
        }

        return dbById.get(dbId);
    }

    public List<NotionPage> getAllPages(String dbId, String sortByFieldId) {
        // No caching here as it is called only when sync work plan
        StringBuilder getPagesPayload = new StringBuilder("{\"page_size\": 100");

        if (!StringUtils.isBlank(sortByFieldId)) {
            getPagesPayload.append(", \"sorts\": [\n" +
                    "\t    {\n" +
                    "\t      \"property\": \""+sortByFieldId+"\",\n" +
                    "\t      \"direction\": \"ascending\"\n" +
                    "\t    }\n" +
                    "\t  ]");
        }

        getPagesPayload.append("}");

        List<NotionPage> results = runPaginatedPost(NotionConstants.API_GET_SINGLE_DB + dbId + "/query", getPagesPayload.toString(), PagesQueryResponse.class, NotionPage.class);

        return results;
    }

    private <T extends NotionObject, P extends PaginatedResponse> List<T> runPaginatedPost(String url, String nonPaginatedJsonPayload, Class<P> paginatedResponseType, Class<T> resultType) {
        boolean hasMoreResults = false;
        String nextCursor = null;

        List<T> results = new ArrayList<>();

        do {
            String jsonPayload = nonPaginatedJsonPayload;
            if (nextCursor != null) {
                JsonObject payload = JsonParser.parseString(nonPaginatedJsonPayload).getAsJsonObject();
                payload.add("start_cursor", new JsonPrimitive(nextCursor));
                jsonPayload = payload.toString();
            }

            ClientResponse response = restClient.sendPost(url, jsonPayload, 200);

            P responseObject = new Gson().fromJson(response.getEntity(String.class), paginatedResponseType);

            results.addAll(Arrays.asList(responseObject.getResults()));

            hasMoreResults = responseObject.has_more;
            nextCursor = responseObject.next_cursor;

        } while (hasMoreResults);

        return results;

    }

    public synchronized void refreshRestConfigIfNeeded(ValueSet config) {
        String currentIntegrationToken = restClient.getIntegrationToken();
        String configIntegrationToken = NotionServiceProvider.getIntegrationToken(config);

        if (!currentIntegrationToken.equals(configIntegrationToken)) {
            // We should use a different Integration Token - Let's reset the REST client and clear all caches.
            NotionRestConfig restConfig = this.restClient.getNotionRestConfig();
            restConfig.setAuthToken(configIntegrationToken);
            this.restClient = new NotionRestClient(restConfig);
            this.allAvailableDatabases = null;
            this.dbById.clear();
        }
    }
}
