AMPS Monitoring Extension
============================

This extension works only with the standalone machine agent.

## Use Case

AMPS is an AMPS is a modern publish and subscribe engine designed specifically for next generation computing environments. It is intended to allow the realization of the scalable high-throughput, low-latency messaging that is required in real-time deployments such as in financial services. This extension allows the user to connect to an AMPS server and retrieve metrics about the performance of the host for the server and the AMPS server instance.  

## Installation

1. Run 'mvn clean install' in the command line from the amps-monitoring-extension directory.
2. Deploy the file AMPSMonitor.zip found in the 'target' directory into `<MACHINE_AGENT_HOME>/monitors/` directory.
3. Unzip the deployed file.
4. Open `<MACHINE_AGENT_HOME>/monitors/AMPSMonitor/config.yml` and configure the AMPS parameters.
  ```

    host: "192.168.1.7"
    port: 8085
    useSSL : false
    username: ""

    #Provide password or passwordEncrypted and encryptionKey
    password: ""

    passwordEncrypted:
    encryptionKey:

    disabledMetrics: [".*client status cache.*"]

    metricPrefix: "Custom Metrics|AMPS|"
    
  ```
5. Restart the machine agent.

In the AppDynamics Metric Browser, look for: Application Infrastructure Performance | &lt;Tier&gt; | Custom Metrics | AMPS

## Directory Structure

| Directory/File | Description |
|----------------|-------------|
|src/main/resources/conf            | Contains the monitor.xml, config.yml |
|src/main/java             | Contains source code of the AMPS monitoring extension |
|target            | Only obtained when using maven. Run 'mvn clean install' to get the distributable .zip file |
|pom.xml       | Maven build script to package the project (required only if changing Java code) |



## Metrics

### Metric Category: host|cpus

|Metric Name            	|Description|
|------------------------------	|------------|
|idle_percent		|Percent of CPU time that the system did not spend waiting on an I/ O request to complete. 
|iowait_percent		|Percent of CPU time spent waiting for I/O requests to complete.  
|system_percent 		|Percent of CPU utilization time which occurred while executing kernel processes. 
|user_percent		|Percent of CPU utilization time which occurred while running at the application level.
 
### Metric Category: host|memory

|Metric Name            	|Description|
|------------------------------	|------------|
|available 	|The total amount of memory available. Calculated as the sum of free, buffers and cached. 
|buffers 	|The amount of physical memory available for file buffers. 
|cached 	|The amount of physical memory used as cache memory. 
|free 		|The amount of physical memory left unused by the system. 
|in_use 	|The amount of memory currently in use. Calculated ask total - (free + buffers + cached).
|swap_free 	|The amount of swap memory which is unused. 
|swap_total 	|The total amount of physical swap memory. 
|total 		|Total amount of RAM.

### Metric Category: host|network

|Metric Name            	|Description|
|------------------------------	|------------|
|bytes_in 	|Number of bytes received by the interface. 
|bytes_out 	|Number of bytes transmitted by the interface. 
|errors 	|Total errors both incoming and outgoing. This number includes packets dropped, collisions, fifo, frame, and carrier errors. 
|packets_in 	|The total number of packets received by the interface. 
|packets_out 	|The total number of packets sent by the interface.

### Metric Category: instance|processors

|Metric Name            	|Description|
|------------------------------	|------------|
|denied_reads 	|Number of read requests which have been denied. 
|denied_writes 	|Number of write requests which have been denied. 
|description 	|Descriptor of the processors resource. 
|last_active 	|Number of seconds since a processor was last active. 
|matches_found 	|Number of messages found. 
|matches_found_per_sec 	|Rate of messages found. 
|messages_received 	|Number of messages received. 
|messages_received_per_sec 	|Rate of messages received. 
|throttle_count 	|Number of times the processor has found no available work.

### Metric Category: instance|cpu

|Metric Name            	|Description|
|------------------------------	|------------|
|denied_reads 	|Number of read requests which have been denied. 
|denied_writes 	|Number of write requests which have been denied. 
|description 	|Descriptor of the processors resource. 
|last_active 	|Number of seconds since a processor was last active. 
|matches_found 	|Number of messages found. 
|matches_found_per_sec 	|Rate of messages found. 
|messages_received 	|Number of messages received. 
|messages_received_per_sec 	|Rate of messages received. 
|throttle_count 	|Number of times the processor has found no available work.
 
### Metric Category: instance|caches

|Metric Name            	|Description|
|------------------------------	|------------|
|allocations 	|Number of memory allocations for this cache. 
|bytes 		|Number of bytes allocated to this cache. 
|efficiency 	|Ratio of hits to requests for this cache. 
|entries 	|Number of entries in this cache. 
|evictions 	|Count of evictions from this cache. 
|fetches 	|Count of fetches from this cache.

### Metric Category: instance|queries

|Metric Name            	|Description|
|------------------------------	|------------|
|queued_queries	|A count of all queries which have not yet completed processing.


##Contributing

Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere]() community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).

