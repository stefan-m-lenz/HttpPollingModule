package de.imbei.httppollingmodule;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author lenzstef
 */
public class ResponseData {
  
    private final int requestId;
    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final String body;

    public ResponseData(HttpResponse<String> response, int requestId) {
        this.requestId = requestId;
        this.statusCode = response.statusCode();
        this.headers = response.headers().map();
        this.body = response.body();
    }
    
    public ResponseData(int requestId, int statusCode, String body) {
        this.requestId = requestId;
        this.statusCode = statusCode;
        this.headers = new HashMap<>();
        this.body = body;
    }

    public int getRequestId() {
        return requestId;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }
    
    public String getBody() {
        return body;
    }
    
    @Override
    public String toString() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(this);
    }
}
