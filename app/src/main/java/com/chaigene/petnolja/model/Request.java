package com.chaigene.petnolja.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Request extends STRObject implements Serializable {
    private static final String RESPONSE_KEY_STATUS = "status";
    private static final String RESPONSE_KEY_CODE = "code";
    private static final String RESPONSE_KEY_MESSAGE = "message";

    public static final int STATUS_SUCCESS = 200;
    public static final int STATUS_FAIL = 400;

    private Map<String, Object> response;

    public Request() {
        response = new HashMap<>();
    }

    public Map<String, Object> getResponse() {
        return response;
    }

    public void setResponse(Map<String, Object> response) {
        this.response = response;
    }

    @Exclude
    public int getResponseStatus() {
        if (response == null) return -1;
        Long status = (Long) response.get(RESPONSE_KEY_STATUS);
        return status.intValue();
    }

    @Exclude
    public void setResponseStatus(int responseStatus) {
        this.response.put(RESPONSE_KEY_STATUS, (long) responseStatus);
    }

    @Exclude
    public int getResponseCode() {
        if (response == null) return -1;
        Long code = (Long) response.get(RESPONSE_KEY_CODE);
        return code.intValue();
    }

    @Exclude
    public String getResponseMessage() {
        if (response == null) return null;
        return (String) response.get(RESPONSE_KEY_MESSAGE);
    }
}