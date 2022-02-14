package de.imbei.httppollingmodule;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 *
 * @author lenzstef
 */
public class ResponseData {
    
    private final int requestId;
    private final Map<String, List<String>> headers;
    private final String body;

    public ResponseData(HttpResponse<String> response, int requestId) {
        this.body = response.body();
        this.requestId = requestId;
        this.headers = response.headers().map();
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
