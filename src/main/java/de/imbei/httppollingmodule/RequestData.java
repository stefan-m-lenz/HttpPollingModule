package de.imbei.httppollingmodule;


import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class RequestData {
    
    private int requestId;
    private String method;
    private String uri;
    private Map<String, List<String>> headers;
    private String body;

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
    
    public HttpRequest buildRequest(Properties config) {
        String targetUrl = config.getProperty("target") + "/" + uri;
        
        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl));
        
        //TODO
//        HttpRequest relayedRequest = HttpRequest.newBuilder()
//                .uri(URI.create(targetUrl)).
                
    }
   
    
}
