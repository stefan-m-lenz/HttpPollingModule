# HttpPollingModule

The `HttpPollingModule` is a Java program, which polls the [`HttpQueueServer`](https://github.com/stefan-m-lenz/HttpQueueServer) for incoming relay requests.
If new requests are in the queue, they are sent to the target server and the answer is sent back to the `HttpQueueServer`.

The program accepts a path to a configuration file as a single argument.
This file is a Java properties file, which contains keys and values.
An example can be found [here](config.properties).
The following configuration options are possible:

|Key|Required|Value|
|-|-|-|
|`queue`|Yes|The base URL of the queue server.|
|`target`|Yes|The base URL of the target server.|
|`timeoutOnFail`|No|If the queue server is not reachable, the polling module will continously retry to establish a connection, pausing a number of seconds after each unsuccesful try. The number of seconds can be specified via this option. The default interval between unsuccesful tries is 30 seconds.|
|`queueWaitingTime`|No|The queue server lets the polling module wait a number of seconds before it tells the polling module that there are no new relay requests. If there is a new relay request coming in during the waiting time, the request can be handed over immediately to the waiting polling module. The polling module passes this option to the server via a query parameter. This parameter tells the queue server the minimum time that the polling module wants to wait . The default value is 30 (seconds).|
|`connectionTimeout`|No|The number of seconds that the polling module waits for a request to be answered. Failed requests are logged. The value must be large enough to account for the queue waiting time. If it is not more than twice the `queueWaitingTime`, it will be adjusted to three times the `queueWaitingTime`.|
|`checkCertificates`|No|If set to `false`, SSL certificates will not be checked.|


## Installation

```{bash}
sudo apt-get update
sudo apt-get upgrade

# Install Java 11
sudo apt-get install openjdk-11-jdk

# Add group and user for tomcat
sudo groupadd tomcat

# Create tomcat user with a home directory of /opt/tomcat and with a shell of /bin/false,
# so nobody can log into the account:

sudo useradd -s /bin/false -g tomcat -d /opt/tomcat tomcat

# Download Tomcat 10
wget https://dlcdn.apache.org/tomcat/tomcat-10/v10.0.16/bin/apache-tomcat-10.0.16.tar.gz

# Create Tomcat installation directory
sudo mkdir /opt/tomcat

# Extract downloaded Tomcat files into installation directorys
sudo tar xzvf apache-tomcat-10*tar.gz -C /opt/tomcat --strip-components=1

# Switch to installation directory
cd /opt/tomcat

# Adapt rights on tomcat directory for tomcat user
sudo chgrp -R tomcat /opt/tomcat
sudo chmod -R g+r conf
sudo chmod g+x conf
sudo chown -R tomcat webapps/ work/ temp/ logs/

sudo update-java-alternatives -l
sudo vi /etc/systemd/system/tomcat.service

sudo systemctl daemon-reload
sudo systemctl start tomcat
sudo systemctl status tomcat
sudo ufw allow 8080 # TODO change wget ...
sudo systemctl enable tomcat


```

## Configuration with nginx

