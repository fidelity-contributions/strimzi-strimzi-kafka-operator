/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.operator.resource.cruisecontrol;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Cruise Control response
 */
public class CruiseControlResponse {
    private final String userTaskId;
    private final JsonNode json;

    /**
     * Constructor
     *
     * @param userTaskId    User task ID
     * @param json          JSON data
     */
    CruiseControlResponse(String userTaskId, JsonNode json) {
        this.userTaskId = userTaskId;
        this.json = json;
    }

    /**
     * @return  User task ID
     */
    public String getUserTaskId() {
        return userTaskId;
    }

    /**
     * @return  The JSON data of the response
     */
    public JsonNode getJson() {
        return json;
    }

    @Override
    public String toString() {
        return "User Task ID: " + userTaskId + " JSON: " + json;
    }
}
