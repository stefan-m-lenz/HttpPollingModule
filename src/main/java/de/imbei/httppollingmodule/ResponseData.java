package de.imbei.httppollingmodule;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.Base64;
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

    public ResponseData(HttpResponse<InputStream> response, int requestId) throws IOException {
        this.requestId = requestId;
        this.statusCode = response.statusCode();
        this.headers = new HashMap<>();
        headers.putAll(response.headers().map());
        headers.remove("transfer-encoding");
        InputStream inputStream = response.body();
        byte[] byteArray = inputStream.readAllBytes();
        this.body = Base64.getEncoder().encodeToString(byteArray);
    }
    
    public ResponseData(int requestId, int statusCode, String body) {
        this.requestId = requestId;
        this.statusCode = statusCode;
        this.headers = new HashMap<>();
        this.headers.put("content-type", List.of("text/plain"));
        this.body = Base64.getEncoder().encodeToString(body.getBytes());
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
