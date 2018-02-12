/*
 * Copyright 2018. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */
package com.appdynamics.extensions.amps;

import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class AMPSWrapper {
    private static final Logger logger = Logger.getLogger(AMPSWrapper.class);
    public static final String METRIC_SEPARATOR = "|";
    public static final String HOST_KEY = "host";
    public static final String INSTANCE_KEY = "instance";
    public static final String ALL_KEY = "all";
    public static final String CPU_KEY = "cpus";
    public static final String MEMORY_KEY = "memory";
    public static final String NETWORK_KEY = "network";
    public static final String CPUI_KEY = "cpu";
    public static final String QUERY_KEY = "queries";
    public static final String CACHE_KEY = "caches";
    public static final String PROCESSOR_KEY = "processors";
    private static final String AMPS_URI = "/amps.json";

    /**
     * Gathers all the amps metrics.  This can be done by a single REST call to host:port/amps.json
     * then we parse out the required metrics from the JSON response
     * we return a map containing as key the metric prefix and as value a map
     * the map value has key of the actual metric name and value the actual metric valus
     *
     * @param httpClient
     * @return
     */
    public Map<String, Double> gatherAMPSMetrics(SimpleHttpClient httpClient) {
        // go issue the REST API Call to get the JSON object
        JsonElement AMPSResponse = getResponse(httpClient, AMPS_URI);

        // create a map object to hold the metrics
        Map<String, Double> ampsMetrics = new HashMap<String, Double>();

        // only process if our REST call found something in JSON format
        if (AMPSResponse != null && AMPSResponse.isJsonObject()) {

            // Get the JSON object rooted at amps
            JsonObject AMPSJson = AMPSResponse.getAsJsonObject().getAsJsonObject("amps");

            // get the Host metrics
            getHostMetrics(AMPSJson, ampsMetrics);

            // get the Instance metrics
            getInstanceMetrics(AMPSJson, ampsMetrics);
        }
        return ampsMetrics;
    }


    /**
     * Returns JsonElement after parsing HttpResponse from given uri
     *
     * @param httpClient - object to make the REST API call with
     * @param uri - the full uri for the REST API call
     * @return REST API data in JSON object form
     */
    private JsonElement getResponse(SimpleHttpClient httpClient, String uri) {
        // go make the REST API call and get the data back as String
        String response = getResponseString(httpClient, uri);

        // Define a JSON element to hold the JSON data
        JsonElement jsonElement = null;

        // parse out the REST API string into JSON formt allowing for invalid JSON data
        // return by the amps server
        try {
            jsonElement = new JsonParser().parse(response);
        } catch (JsonParseException e) {
            logger.error("Response from " + uri + "is not a json");
        }

        return jsonElement;
    }

    /**
     * Returns HttpResponse as string from given url
     *
     * @param httpClient
     * @param path
     * @return
     */
    private String getResponseString(SimpleHttpClient httpClient, String path) {

        // default the data as null
        Response response = null;
        String responseString = "";

        // go execute the REST API catching any exceptions
        try {
            response = httpClient.target().path(path).get();
            responseString = response.string();
        } catch (Exception e) {
            logger.error("Exception in getting response from " + path, e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (Exception ex) {
                // Ignore
            }
        }
        return responseString;
    }

    /**
     * Populates the amps Host metrics hashmap
     *
     * @param AMPSJson - The amps Json object
     * @param ampsMetrics - the map containing all the host metrics
     * @return
     */
    private void getHostMetrics(JsonObject AMPSJson, Map<String, Double> ampsMetrics) {

        // extract the host JSON data
        JsonObject hostJson = AMPSJson.getAsJsonObject(HOST_KEY);

        // extract the cpu subset of host and add to our metrics map
        Map<String, Double> metrics = getArrayMetricsByID(hostJson, CPU_KEY, ALL_KEY);

        populateMap(HOST_KEY + METRIC_SEPARATOR + CPU_KEY, metrics, ampsMetrics);

        //ampsMetrics.put(HOST_KEY + METRIC_SEPARATOR + CPU_KEY, metrics);

        // extract the memory subset of host and add to our metrics map
        metrics = getRootMetrics(hostJson, MEMORY_KEY);
        populateMap(HOST_KEY + METRIC_SEPARATOR + MEMORY_KEY, metrics, ampsMetrics);

        // extract the network subset of host and add to our metrics map
        metrics = getArrayMetrics(hostJson, NETWORK_KEY, "id");
        populateMap(HOST_KEY + METRIC_SEPARATOR + NETWORK_KEY, metrics, ampsMetrics);

    }

    private void populateMap(String metricCategory, Map<String, Double> metrics, Map<String, Double> ampsMetrics) {

        for (Entry<String, Double> entry : metrics.entrySet()) {
            ampsMetrics.put(metricCategory + METRIC_SEPARATOR + entry.getKey(), entry.getValue());
        }
    }

    /**
     * Populates the amps Host metrics hashmap
     *
     * @param AMPSJson - The amps Json object
     * @param ampsMetrics - the map containing all the host metrics
     * @return
     */
    private void getInstanceMetrics(JsonObject AMPSJson, Map<String, Double> ampsMetrics) {

        // extract the instance JSON data
        JsonObject instanceJson = AMPSJson.getAsJsonObject(INSTANCE_KEY);

        // extract the cpu subset of instance and add to our metrics map
        Map<String, Double> metrics = getRootMetrics(instanceJson, CPUI_KEY);
        populateMap(INSTANCE_KEY + METRIC_SEPARATOR + CPUI_KEY, metrics, ampsMetrics);

        // extract the cache subset of instance and add to our metrics map
        JsonObject memoryJson = instanceJson.getAsJsonObject(MEMORY_KEY);
        JsonObject cacheJson = memoryJson.getAsJsonObject(CACHE_KEY);
        metrics = getArrayMetrics(cacheJson, CACHE_KEY, "description");
        populateMap(INSTANCE_KEY + METRIC_SEPARATOR + CACHE_KEY, metrics, ampsMetrics);

        // extract the query subset of instance and add to our metrics map
        metrics = getRootMetrics(instanceJson, QUERY_KEY);
        populateMap(INSTANCE_KEY + METRIC_SEPARATOR + QUERY_KEY, metrics, ampsMetrics);

        // extract the processor subset of instance and add to our metrics map
        metrics = getArrayMetricsByID(instanceJson, PROCESSOR_KEY, ALL_KEY);
        populateMap(INSTANCE_KEY + METRIC_SEPARATOR + PROCESSOR_KEY, metrics, ampsMetrics);
    }

    /**
     * Returns a map of the CPU metrics
     *
     * @param hostJson - the extracted amps Json object
     * @return Map of CPU metrics
     */
    private Map<String, Double> getArrayMetrics(JsonObject hostJson, String key, String id) {

        // extract the array for the caller's key
        JsonArray arrayJson = hostJson.getAsJsonArray(key);

        // define a map to hold the metrics
        Map<String, Double> metrics = new HashMap<String, Double>();

        // loop thru the array extracting the metrics
        Iterator<JsonElement> it = arrayJson.iterator();
        while (it.hasNext()) {
            // get the next json object from the array
            JsonElement elementJson = it.next();
            JsonObject oJson = elementJson.getAsJsonObject();

            // loop thru the CPU elements looking for id to add to the metric
            Iterator<Entry<String, JsonElement>> it1 = oJson.entrySet().iterator();
            while (it1.hasNext()) {
                Entry<String, JsonElement> entry = it1.next();
                String metricName = entry.getKey();
                JsonElement JsonValue = entry.getValue();
                String value = JsonValue.getAsString();
                if (metricName.equals(id) && JsonValue instanceof JsonPrimitive) {
                    it1 = oJson.entrySet().iterator();
                    populateMetricsMapHelper(it1, metrics, value + METRIC_SEPARATOR);
                }
            }
        }

        return metrics;
    }

    /**
     * Returns a map of the CPU metrics
     *
     * @param hostJson - the extracted amps Json object
     * @return Map of CPU metrics
     */
    private Map<String, Double> getArrayMetricsByID(JsonObject hostJson, String key, String id) {

        // extract the cpu subset of host
        JsonArray arrayJson = hostJson.getAsJsonArray(key);

        // define a map to hold the metrics
        Map<String, Double> metrics = new HashMap<String, Double>();

        // loop thru the CPU IDs until we find caller's id then put its metrics out
        Iterator<JsonElement> it = arrayJson.iterator();
        boolean allFound = false;
        while (it.hasNext() && !allFound) {
            // get the next json object from the array
            JsonElement elementJson = it.next();
            JsonObject oJson = elementJson.getAsJsonObject();

            // loop thru the elements looking for id
            Iterator<Entry<String, JsonElement>> it1 = oJson.entrySet().iterator();
            while (it1.hasNext()) {
                // get the next element
                Entry<String, JsonElement> entry = it1.next();

                // gets its name and value
                String metricName = entry.getKey();
                JsonElement value = entry.getValue();

                // check we have valid json, and if its the id we were asked to select
                if (metricName.equals("id") && value instanceof JsonPrimitive && value.getAsString().equals(id)) {
                    // get an iterator for the populate call and go grab the metrics for the
                    // caller's id
                    it1 = oJson.entrySet().iterator();
                    populateMetricsMapHelper(it1, metrics, "");
                    // done so flag to exit
                    allFound = true;
                }
            }
        }

        return metrics;
    }

    /**
     * Returns a map of the Memory metrics
     *
     * @param hostJson - the extracted amps Json object
     * @return Map of Memory metrics
     */
    private Map<String, Double> getRootMetrics(JsonObject hostJson, String key) {

        // extract the root Json object based on caller's key
        JsonObject rootJson = hostJson.getAsJsonObject(key);

        // define a map to hold the metrics
        Map<String, Double> metrics = new HashMap<String, Double>();

        // Go get the metrics from the Json object
        Iterator<Entry<String, JsonElement>> it = rootJson.entrySet().iterator();
        populateMetricsMapHelper(it, metrics, "");

        // pass back to the caller
        return metrics;
    }

    /**
     * Populates a map with values retrieved from the entry set of a Json
     * Object
     *
     * @param iterator
     *            An entry set iterator for the json object
     * @param metrics
     *            Map that is populated based on the values retrieved from entry set
     * @param prefix
     *            Optional prefix for the metric name to distinguish duplicate
     *            metric names
     */
    private void populateMetricsMapHelper(Iterator<Entry<String, JsonElement>> iterator, Map<String, Double> metrics, String prefix) {
        while (iterator.hasNext()) {
            Entry<String, JsonElement> entry = iterator.next();
            String metricName = entry.getKey();
            JsonElement value = entry.getValue();
            if (value instanceof JsonPrimitive && NumberUtils.isNumber(value.getAsString())) {
                Double val = value.getAsDouble();
                metrics.put(prefix + metricName, val);
            }
        }
    }
}
