# HttpPollingModule

The `HttpPollingModule` is a Java program, which polls the [`HttpQueueServer`](https://github.com/stefan-m-lenz/HttpQueueServer) for incoming relay requests.
If new requests are in the queue, they are sent to the target server and the answer is sent back to the `HttpQueueServer`.

With Java 11 installed, e.g. via `sudo apt-get install openjdk-11-jre`, the polling module can be run via:

```
java -jar HttpPollingModule.jar config.properties
```

If you want to deploy the polling module in production, the Java application should be wrapped into a service or put into a restarting Docker container.

The program accepts a path to a configuration file as a single argument.
This file is a Java properties file, which contains keys and values.
An example can be found [here](config.properties).
The following configuration options are possible:

|Key|Required|Value|
|-|-|-|
|`queue`|Yes|The base URL of the queue server.|
|`target`|Yes|The base URL of the target server.|
|`queueProxy`|No|If a proxy server is needed to access the `HttpQueueServer`, the address of the proxy can be specified via this argument in the form *hostname*:*port*.|
|`queueClientAuthCert`|No|If client authorization is used to authenticate the polling module at the queue, the path to a PKCS12-file that contains the certificate can be specified.|
|`queueClientAuthCertPassword`|No|The password for the `queueClientAuthCert` certificate file|
|`timeoutOnFail`|No|If the queue server is not reachable, the polling module will continously retry to establish a connection, pausing a number of seconds after each unsuccesful try. The number of seconds can be specified via this option. The default interval between unsuccesful tries is 30 seconds.|
|`queueWaitingTime`|No|The queue server lets the polling module wait a number of seconds before it tells the polling module that there are no new relay requests. If there is a new relay request coming in during the waiting time, the request can be handed over immediately to the waiting polling module. The polling module passes this option to the server via a query parameter. This parameter tells the queue server the minimum time that the polling module wants to wait . The default value is 30 (seconds).|
|`connectionTimeout`|No|The number of seconds that the polling module waits for a request to be answered. Failed requests are logged. The value must be large enough to account for the queue waiting time. If it is not more than twice the `queueWaitingTime`, it will be adjusted to three times the `queueWaitingTime`.|
|`checkCertificates`|No|If set to `false`, SSL certificates will not be checked.|

## Run HttpPollingModule as a service
```bash
# Create a system user group
   sudo groupadd -r group_name
   
# Create a system user with the group.
  sudo useradd -r -s /bin/false -g group_name user_name

# Confirm if user created and is in correct group
  Id  user_name

# Create a systemd service file to manage the application
  sudo vim /etc/systemd/system/HttpPollingModule.service

# Enter the following text in the HttpPollingModule.service file
# Replace the path for WorkingDirectory, Java_path, User to fit your configurations

[Unit]
Description=Manage polling service

[Service]
WorkingDirectory=/path/to/folder/with/HttpPollingModule.jar
ExecStart=/Java_path/bin/java -jar HttpPollingModule.jar config.properties
User=user_name
Type=simple

[Install]
WantedBy=multi-user.target

# Grant the user and group ownership permissions for the Project Directory:
 sudo chown -R user_name:group_name /path/to/folder/with/HttpPollingModule.jar

# Start the HttpPollingModule.service
  sudo systemctl daemon-reload
  sudo systemctl start HttpPollingModule.service
  sudo systemctl status HttpPollingModule.service

# If Active: failed (Result: exit-code), make sure you are able to run /*Java_path*/bin/java -jar HttpPollingModule.jar config.properties in your command line
```
