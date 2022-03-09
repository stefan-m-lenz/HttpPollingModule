# HttpPollingModule

The `HttpPollingModule` is a Java program, which polls the [`HttpQueueServer`](https://github.com/stefan-m-lenz/HttpQueueServer) for incoming relay requests.
If new requests are in the queue, they are sent to the target server and the answer is sent back to the `HttpQueueServer`.

With Java 11 installed, e.g. via `sudo apt-get install openjdk-11-jre`, the polling module can be run via:

```
java -jar HttpPollingModule.jar config.properties
```

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

## Deployment as a service

For deploying the polling module in production, the Java application can be wrapped into a service.
The following steps show how this can be done on an Ubuntu 18.04 server:

Create a system user group with a user that runs the service:

```bash
sudo groupadd -r pollGroup
sudo useradd -r -s /bin/false -g pollGroup pollUser
```

Create an installation directory and download the application:

```bash
sudo mkdir /opt/poll
wget https://github.com/stefan-m-lenz/HttpPollingModule/releases/download/v1.0/HttpPollingModule.jar
sudo mv HttpPollingModule.jar /opt/poll
```

Set up a `config.properties` file and put it in the directory.
An example file can be found [here](config.properties.example).

If client authentication is required to secure the endpoints for the polling module in the queue server,
the client certificate created during the [setup of queue server](https://github.com/stefan-m-lenz/HttpQueueServer#install-and-configure-nginx-as-reverse-proxy) can also be placed in this folder.

If not already done, install Java 11:

```bash
sudo apt-get update
sudo apt-get install openjdk-11-jre
```

Then create a systemd service file to manage the application:

```bash
sudo vim /etc/systemd/system/HttpPollingModule.service
```

The file should contain the following text:

```
[Unit]
Description=Manage polling service

[Service]
WorkingDirectory=/opt/poll
ExecStart=/usr/bin/java -jar HttpPollingModule.jar config.properties
User=pollUser
Type=simple

[Install]
WantedBy=multi-user.target
```

Check whether your Java installation path is `/usr/bin/java` by running `which java`.
If it is different, replace it in the file.

Grant user permission on the directory to the user for running the polling module:

```bash
sudo chown -R pollUser:pollGroup /opt/poll
```

Start the service:

```bash
sudo systemctl daemon-reload
sudo systemctl start HttpPollingModule.service
```

Check the status of the service:

```bash
systemctl status HttpPollingModule.service
```

The status should be "Active (running)".