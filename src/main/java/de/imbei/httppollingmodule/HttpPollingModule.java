package de.imbei.httppollingmodule;

import com.google.gson.Gson;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


public class HttpPollingModule {
    
    public static final String VERSION = "0.1";

    public static Properties getConfig(String fileName) throws FileNotFoundException, IOException {
        Properties config = new Properties();
        try (FileInputStream fis = new FileInputStream(fileName)) {
            config.load(fis);
        }
        return config;
    }
    
    public static Properties handleCommandLineArgs(String[] args) {
        if (args.length == 0) {
            System.err.println("Supply path to config file as argument");
            System.exit(1);
        }
        
        if ("--version".equals(args[0])) {
            System.out.println(VERSION);
            System.exit(1);
        }
        
        Properties config;
        try {
            config = getConfig(args[0]);
        } catch (IOException ex) {
            Logger.getLogger(HttpPollingModule.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Could not read config file");
            System.exit(1);
            return null;
        }
        return config;
    }
   
    private static RequestData tryFetchRequest(String requestPath) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request4request = HttpRequest.newBuilder()
                .uri(URI.create(requestPath))
                .GET()
                .build();
        
        HttpResponse<String> response = client.send(request4request, 
                BodyHandlers.ofString());
        String responseBody = response.body();
        if ("".equals(responseBody)) { // no new requests
            return null;
        } else {
            Gson gson = new Gson();
            return gson.fromJson(responseBody, RequestData.class);
        }
    }
    
    private static ResponseData relayRequest(String targetPath, RequestData requestData) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        
        BodyPublisher bodyPublisher;
        if ("POST".equals(requestData.getMethod()) || "PUT".equals(requestData.getMethod())) {
            bodyPublisher = BodyPublishers.ofString(requestData.getBody());
        } else {
            bodyPublisher = BodyPublishers.noBody();
        }
        
        HttpRequest relayedRequest = HttpRequest.newBuilder()
                .uri(URI.create(targetPath + requestData.getUri()))
                .method(requestData.getMethod(), bodyPublisher)
                .build();
        
        HttpResponse<String> response = client.send(relayedRequest,
                BodyHandlers.ofString());
        
        return new ResponseData(response, requestData.getRequestId());
    }
    
    private static void postResponse(ResponseData responseData) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest responseRequest = HttpRequest.newBuilder()
                .uri(URI.create("response" + responseData.getRequestId()))
                .method("POST", BodyPublishers.ofString(responseData.toString()))
                .build();
        client.send(responseRequest, BodyHandlers.discarding());
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        
        Properties config = handleCommandLineArgs(args);
        String targetPath = config.getProperty("target");
        if (!targetPath.endsWith("/")) {
            targetPath = targetPath + "/";
        }
        String requestPath = config.getProperty("queue") + "/pop-request";
        
        // TODO loop, handle exceptions
        RequestData requestData = tryFetchRequest(requestPath);
        
        // TODO start in thread: https://stackoverflow.com/a/20710164/3180809
        ResponseData responseData = relayRequest(targetPath, requestData);
        postResponse(responseData);  
        
    }
}
