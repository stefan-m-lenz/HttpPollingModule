package de.imbei.httppollingmodule;

import java.net.URI;

/**
 *
 * @author lenzstef
 */
public class RequestHandler implements Runnable {

    private final RequestData requestData;
    private final String targetPath;
    private final URI responseUri;
    
    public RequestHandler(RequestData requestData, String targetPath, URI responseUri) {
        this.requestData = requestData;
        this.targetPath = targetPath;
        this.responseUri = responseUri;
    }
    
    @Override
    public void run() {
        HttpPollingModule.processRequest(requestData, targetPath, responseUri);
    }
    
}
