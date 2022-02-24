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
import java.util.regex.Pattern;
import static java.util.stream.Collectors.toList;

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
        if (headers.containsKey("location")) {
            headers.put("location", headers.get("location").stream()
                    .map(el -> el.replaceFirst("^" + Pattern.quote(HttpPollingModule.targetPath), 
                    //    HttpPollingModule.queuePath
                    "http://localhost:8080/QueueServer/relay/" //TODO property "relay"
                    )).collect(toList()));
        }
        InputStream inputStream = response.body();
        byte[] byteArray = inputStream.readAllBytes();
        this.body = Base64.getEncoder().encodeToString(byteArray);
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
