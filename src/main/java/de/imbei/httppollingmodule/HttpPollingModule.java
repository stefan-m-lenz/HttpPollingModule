/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.imbei.httppollingmodule;

import com.google.gson.Gson;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
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
   

    public static void main(String[] args) {
        
        Properties config = handleCommandLineArgs(args);

        String requestPath = config.getProperty("queue") + "/request";
        String targetPath = config.getProperty("target");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request4request = HttpRequest.newBuilder()
                .uri(URI.create(requestPath))
                .GET()
                .build();
        
        try {
            HttpResponse<String> response = client.send(request4request, 
                    BodyHandlers.ofString());
            String responseBody = response.body();
            if ("".equals(responseBody)) { // no new requests
                return;
            } else {
                Gson gson = new Gson();
                RequestData requestData = gson.fromJson(responseBody, RequestData.class);
                HttpRequest targetRequest = requestData.buildRequest(config);
                client.send(targetRequest, BodyHandlers.ofString());
            }
            
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(HttpPollingModule.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
