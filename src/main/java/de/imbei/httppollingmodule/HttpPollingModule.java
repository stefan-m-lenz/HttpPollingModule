package de.imbei.httppollingmodule;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;


public class HttpPollingModule {
    
    public static final String VERSION = "0.1";
    public static final int DEFAULT_TIMEOUT_ON_FAIL_MILLIS = 30000;
    public static int DEFAULT_QUEUE_WAITING_TIME_SECONDS = 30;
    private static final Logger logger = Logger.getLogger("Polling status");
    private static long connectionTimeout = 90;

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
   
    /**
     * Sends an HTTP request to the queue server to check for a new relay request.
     * @param requestUri the URL of the relay server queue
     * @return a new client relay request or 
     * null if no client relay request was made to the relay server 
     * while executing the request to the request queue
     * @throws IOException
     * @throws InterruptedException 
     */
    private static RequestData tryFetchRequest(URI requestUri) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        
        HttpRequest request4request = HttpRequest.newBuilder()
                .uri(requestUri)
                .POST(BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(connectionTimeout))
                .build();
        
        HttpResponse<String> response = client.send(request4request, 
                BodyHandlers.ofString());
        String responseBody = response.body();
        if ("".equals(responseBody)) { // no new requests
            return null;
        } else {
            Gson gson = new Gson();
            try {
                return gson.fromJson(responseBody, RequestData.class);
            } catch (JsonSyntaxException ex) {
                logger.log(Level.SEVERE, "Answer of server could not be parsed to RequestData object.\n" +
                        "Answer from server: \n" + responseBody);
                return null;
            }
        }
    }
    
    private static RequestData fetchRequestData(URI requestUri, int timeoutOnFail) throws InterruptedException {
        RequestData requestData = null;
        while (requestData == null) {
            try {
                requestData = tryFetchRequest(requestUri);
            } catch (IOException | InterruptedException ex) {
                logger.info("Fetching request failed, queue server could not be reached");
                Thread.sleep(timeoutOnFail);
            }
        }
        return requestData;
    }
    
    private static ResponseData relayRequest(String targetPath, RequestData requestData) throws IOException, InterruptedException {
        HttpClient client = null;
        try {
            client = HttpClient.newBuilder()
                    .sslContext(SSLContext.getDefault())
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
        } catch (NoSuchAlgorithmException ex) {
            logger.log(Level.SEVERE, "Creating HTTP client fails", ex);
            System.exit(1);
        }
        
        BodyPublisher bodyPublisher;
        if ("POST".equals(requestData.getMethod()) || "PUT".equals(requestData.getMethod())) {
            bodyPublisher = BodyPublishers.ofString(requestData.getBody());
        } else {
            bodyPublisher = BodyPublishers.noBody();
        }
        
        HttpRequest relayedRequest = HttpRequest.newBuilder()
                .uri(URI.create(targetPath + requestData.getUri()))
                .timeout(Duration.ofSeconds(connectionTimeout))
                .method(requestData.getMethod(), bodyPublisher)
                .build();
        
        HttpResponse<String> response = client.send(relayedRequest,
            BodyHandlers.ofString());
        return new ResponseData(response, requestData.getRequestId());
    }
    
    private static void postResponse(URI responseUri, ResponseData responseData) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest responseRequest = HttpRequest.newBuilder()
                .uri(responseUri)
                .timeout(Duration.ofSeconds(connectionTimeout))
                .method("POST", BodyPublishers.ofString(responseData.toString()))
                .build();
        
        try {
            client.send(responseRequest, BodyHandlers.discarding());
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Error while posting response", ex);
        }
    }
    
    public static void processRequest(RequestData requestData, String targetPath, URI responseUri) {
        ResponseData responseData;
        try {
            responseData = relayRequest(targetPath, requestData);
        } catch (HttpConnectTimeoutException ex) {
            logger.log(Level.WARNING, "Timeout connecting with target server", ex);
            responseData = new ResponseData(requestData.getRequestId(), 408, "Timeout while executing request");
        } catch (IOException | InterruptedException ex) {
            logger.log(Level.SEVERE, "Fetching request failed, target server could not be reached", ex);
            responseData = new ResponseData(requestData.getRequestId(), 500, "Error executing request");
        }
        
        try {
            postResponse(responseUri, responseData);
        } catch (IOException | InterruptedException ex) {
            logger.log(Level.SEVERE, "Response could not be delivered", ex);
        }
    }
    
    public static void main(String[] args) throws NoSuchAlgorithmException, InterruptedException {
        
        Properties config = handleCommandLineArgs(args);
        
        String targetPath = config.getProperty("target");
        if (!targetPath.endsWith("/")) {
            targetPath = targetPath + "/";
        }
        
        String queuePath = config.getProperty("queue");
        if (!queuePath.endsWith("/")) {
            queuePath = queuePath + "/";
        }
        
        int queueWaitingTime = DEFAULT_QUEUE_WAITING_TIME_SECONDS;
        if (config.getProperty("queueWaitingTime") != null) {
            queueWaitingTime = Integer.parseInt(config.getProperty("queueWaitingTime"));    
        }
        
        if (config.getProperty("connectionTimeout") != null) {
            connectionTimeout = Integer.parseInt(config.getProperty("connectionTimeout"));
        }
        
        if (connectionTimeout <= 2*queueWaitingTime) {
            connectionTimeout = 3*queueWaitingTime;
            logger.info("Value of connectionTimeout too small - value adjusted");
        }
        
        URI requestUri = URI.create(queuePath + "pop-request?w="+queueWaitingTime);
        URI responseUri = URI.create(queuePath + "response");
        
        int timeoutOnFail = DEFAULT_TIMEOUT_ON_FAIL_MILLIS;
        if (config.getProperty("timeoutOnFail") != null) {
            timeoutOnFail = 1000 * Integer.parseInt(config.getProperty("timeoutOnFail"));    
        }

        while (true) {
            RequestData requestData = fetchRequestData(requestUri, timeoutOnFail);
            new Thread(new RequestHandler(requestData, targetPath, responseUri)).start();
        }
        
    }
}
