# Rightscale Nodes Plugin

Generates Rundeck Nodes from the operational Instances associated
with the Servers and ServerArrays in your RightScale account.

Much of the data seen on the "Info" tab in the Rightscale dashboard becomes available as Node attributes and tags.
The plugin uses the Rightscale 1.5 API to retrieve the necessary resources to populate the Rundeck Nodes
(see [API reference](http://reference.rightscale.com/api1.5/index.html),
[API guide](http://support.rightscale.com/12-Guides/RightScale_API_1.5)).

To make the plugin more efficient and not place undue demands on the Rightscale API server,
the plugin caches data and refreshes the data at a configured time interval.

**Note**: Requires Rundeck 2.0

## Build

    ./gradlew clean build

## Install

Copy the `rightscale-nodes-x.y.jar` file to the `libext/` directory inside your Rundeck installation.


## Configuration

The plugin has a number of configuration options needed to connect and produce Rundeck Nodes.
Use the Configure page in the GUI to set them up initially. They roughly are organized:

Connection
* `Email`: String, Email address for RightScale User.
* `Password`: String, Password for RightScale User.
* `Endpoint`: String, RightScale  API Endpoint URL. Must support API v1.5.
* `Refresh Interval`: Integer, Minimum time in seconds between API requests to RightScale (default is 60).

Mapping
* `Default Node Username`: String, Default SSH username for remote execution.
* `Input Pattern`: String, Regular expression used to match
[ResourceInputs](http://reference.rightscale.com/api1.5/resources/ResourceInputs.html) (default is .*).
* `Tag Pattern`: String, Regular expression used to match
[ResourceTags](http://reference.rightscale.com/api1.5/resources/ResourceTags.html) (default is .*).
* `Generate attributes from tags`: Boolean, For tags that contain an equal sign (foo=bar), generate a like node attribute.

Debug
* `HTTP timeout`: Integer, Cause an error if HTTP connect or read requests fail after the specified milliseconds. (Set it to 0 for infinite interval).
* `HTTP request logging`: Boolean, Print debug HTTP request info and the content of the response to service.log.
* `Metrics logging interval`: Integer, Log the codahale metrics to the service.log file at the specified minute interval (no logging if unset).

*Warning*: Changing any plugin configuration property will cause the plugin to be reloaded and a complete refresh.

## Resource model

Much of the data seen on the "Info" tab of the Rightscale dashboard becomes available as Node attributes and tags
to Rundeck. A Rightscale Server or ServerArray instance is actually defined in terms of other Rightscale Resources.

The plugin makes an HTTP request for each of the Rightscale Resources.
At this time, the plugin retrieves the following kinds of Rightscale resources when generating Rundeck nodes:
[Clouds](http://reference.rightscale.com/api1.5/resources/ResourceClouds.html),
[Datacenters](http://reference.rightscale.com/api1.5/resources/ResourceDatacenters.html),
[Deployments](http://reference.rightscale.com/api1.5/resources/ResourceDeployments.html),
[Images](http://reference.rightscale.com/api1.5/resources/ResourceDeployments.html),
[Inputs](http://reference.rightscale.com/api1.5/resources/ResourceInputs.html),
[InstanceTypes](http://reference.rightscale.com/api1.5/resources/ResourceInstanceTypes.html),
[Instances](http://reference.rightscale.com/api1.5/resources/ResourceInstances.html),
[ServerArrays](http://reference.rightscale.com/api1.5/resources/ResourceServerArrays.html),
[ServerTemplates](http://reference.rightscale.com/api1.5/resources/ResourceServerTemplates.html),
[Servers](http://reference.rightscale.com/api1.5/resources/ResourceServers.html),
[SshKeys](http://reference.rightscale.com/api1.5/resources/ResourceSshKeys.html),
[Subnets](http://reference.rightscale.com/api1.5/resources/ResourceSubnets.html),
[Tags](http://reference.rightscale.com/api1.5/resources/ResourceTags.html)
.

The plugin supports a few conventions to map the multiple resources to Rundeck Nodes.
The data for each type of resource is prefixed with its own key. For example,
cloud data is prefixed with `cloud`. Dots separate the prefix from the attribute name.

Node attributes are given names using the following convention: `{{prefix}}.{{attribute}}`

For example, a Cloud's cloud_type value becomes a Node attribute called: `cloud.cloud_type`.
Some resource data is used to generate Rundeck Tags.
Tags are generated to represent several Resource memberships useful for Rundeck Node filtering.
(e.g, for datacenter.name, cloud.name, deployment.name).


### Rundeck dispatch attributes

The builtin SSH NodeExecutor expects several essential attributes about your nodes.
The plugin uses the following values for these attributes:

* Username: The plugin configuration property "Default Node Username" (required).
* Hostname: Automatically set to the value of `instance.public_ip_address`
* OS family: (default: 'unix').

Mostly used for informational reasons, the following attributes are also set.
* Description: Automatically set to the value of `instance.description`
* OS name: (default: 'Linux')
* OS version: (default: '')
* OS arch: (default:'x86_64)

Some of these defaults could be replaced by Rightscale resource data but more convention
is required. Log an issue, if interested.

### Inputs

Rightscale [Resource Inputs](http://reference.rightscale.com/api1.5/resources/ResourceInputs.html)
for an instance are mapped to Rundeck Node attributes using a simple naming convention.
Each input is prefixed with the key, `inputs`, followed by the input name. If the input
 name contains a "/" (slash) a "." (dot) will replace it. For example, the input named,
"rs_utils/process_list" will be mapped to the attribute `inputs.rs_utils.process_list`.

Use the "Input Pattern" configuration setting to declare a regular expression to
match the inputs you wish to see as Rundeck Node attributes.

### Tags

Rightscale [Tags](http://reference.rightscale.com/api1.5/resources/ResourceTags.html)
can be mapped directly to Rundeck Tags.

Use the "Tag Pattern" configuration setting to declare a regular expression to
match the Rightscale tags you wish to see as Rundeck node tags.

Rundeck tags often represent a classification or a grouping but
Rightscale (and possibly your team) might use Rightscale tags to define key/value
pairs as extra metadata about your Instances
(eg, `node:resolvers=172.16.0.23, rs_monitoring:state=active`).

If an equal sign character is contained in the tag value, you might wish to generate
it as a Rundeck node attribute instead.

Check the "Generate attributes from tags" configuration setting to map tags containing an equal
sign to Rundeck node attributes.
If configured, a Rightscale tag value, `node:resolvers=172.16.0.23`, becomes a Rundeck
attribute:

```
<attribute name='tags.node:resolvers' value='172.16.0.23'/>
```


## Refresh

After the plugin starts, it loads a complete set of data for all the
Instances and linked resources for the Servers and ServerArrays.
After this initialization, the plugin refreshes data
at a time interval specified by the configuration property, `Refresh Interval`.

To traverse the data linked to by the Instances, the plugin makes multiple HTTP requests
to the Rightscale API to gather the needed Resources and stores it in an internal cache.

During each refresh, the plugin generates a Node set from the API request data stored in the cache.



### Cache priming

Rightscale instances are linked to by a number of common resources.
This basic data is used to prime the cache.

* Clouds
* Deployments
* ServerTemplates
* Datacenters
* InstanceTypes
* Subnets

Because this data changes infrequently, it is only loaded once. Restart the plugin,
 or change its configuration to completely reload the cache.

### Every refresh

* Instances
* SshKeys
* Images
* Tags
* Inputs
* Servers
* ServerArrays

## Errors

See the service.log for any error messages produced by the plugin.
The most likely source of errors are failed API requests. This might
be due to service unavailability, authentication/authorization,
or unanticipated API behavior.

If the plugin experiences an uncaught error, Rundeck will return the data
from the last successful refresh. Unnoticed, this will lead to stale
data which may cause problems and prevent you from seeing Instance changes.

See the `RightscaleAPIRequest.request.fail` metric mentioned in the Metrics section.
Any HTTP error will increment that counter.

A high number of failures is mostly likely due to an authentication error.
The `RightscaleAPIRequest.authentication` metric that increments a count
each time an authentication request is made.

## Metrics

The plugin uses the codahale metrics library to keep a number of statistics
important for better tuning and stabilizing the plugin operation.

In the Rundeck configuration page, specify a value for the
'Metrics interval' configuration property. After each specified
number of minutes elapses, the metrics will be written to the service.log.

Metrics:

* `RightscaleNodes.nodes.count`: Number of Nodes generated in the last refresh.
* `RightscaleAPIRequest.authentication`: Count authentication requests made to establish a session.
* `RightscaleAPIRequest.fail`: Count of failed API requests.
* `RightscaleAPIRequest.success`: Count of successful API requests.
* `RightscaleAPIRequest.request.duration`: Timer for API requests.
* `RightscaleNodes.refresh.rate`: Rate of Number of nodes / Refresh time.
* `RightscaleNodes.getServerArrayNodes.duration`: Time taken to generate Nodes for the Instances in the ServerArrays
* `RightscaleNodes.getServerNodes.duration`: Time taken to generate Nodes for the Instances in the Servers
* `RightscaleNodes.loadCache.cloud.{{cloud_id}}.instances.duration`: Time taken to to load Instance data for the scecified cloud.
* `RightscaleNodes.loadCache.duration`: Time taken to make API requests and load results in cache.
* `RightscaleNodes.populateLinkedResources.duration`: Time taken to populate a single Node from linked Instance data.
* `RightscaleNodes.refresh.duration`: Time to refresh the cache and generate Nodes (milliseconds)

File a Github Issue if you have ideas about other useful metrics.

Example metrics output.

```
-- Gauges ----------------------------------------------------------------------
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.nodes.count
             value = 2

-- Counters --------------------------------------------------------------------
com.simplifyops.rundeck.plugin.resources.RightscaleAPIRequest.authentication
             count = 2
com.simplifyops.rundeck.plugin.resources.RightscaleAPIRequest.request.fail
             count = 0
com.simplifyops.rundeck.plugin.resources.RightscaleAPIRequest.request.success
             count = 1338

-- Meters ----------------------------------------------------------------------
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.refresh.rate
             count = 56
         mean rate = 0.02 events/second
     1-minute rate = 0.05 events/second
     5-minute rate = 0.02 events/second
    15-minute rate = 0.04 events/second

-- Timers ----------------------------------------------------------------------
com.simplifyops.rundeck.plugin.resources.RightscaleAPIRequest.request.duration
             count = 669
         mean rate = 0.29 calls/second
     1-minute rate = 0.42 calls/second
     5-minute rate = 0.23 calls/second
    15-minute rate = 0.21 calls/second
               min = 183.48 milliseconds
               max = 26618.75 milliseconds
              mean = 1283.01 milliseconds
            stddev = 3170.33 milliseconds
            median = 694.65 milliseconds
              75% <= 832.22 milliseconds
              95% <= 3170.35 milliseconds
              98% <= 16311.65 milliseconds
              99% <= 21012.37 milliseconds
            99.9% <= 26618.75 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.getServerArrayNodes.duration
             count = 18
         mean rate = 0.01 calls/second
     1-minute rate = 0.01 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.02 calls/second
               min = 3.72 milliseconds
               max = 14.58 milliseconds
              mean = 4.91 milliseconds
            stddev = 2.47 milliseconds
            median = 4.25 milliseconds
              75% <= 4.70 milliseconds
              95% <= 14.58 milliseconds
              98% <= 14.58 milliseconds
              99% <= 14.58 milliseconds
            99.9% <= 14.58 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.getServerNodes.duration
             count = 18
         mean rate = 0.01 calls/second
     1-minute rate = 0.01 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.02 calls/second
               min = 4.48 milliseconds
               max = 39.23 milliseconds
              mean = 7.51 milliseconds
            stddev = 7.94 milliseconds
            median = 5.72 milliseconds
              75% <= 6.07 milliseconds
              95% <= 39.23 milliseconds
              98% <= 39.23 milliseconds
              99% <= 39.23 milliseconds
            99.9% <= 39.23 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.loadCache.cloud.1.instances.duration
             count = 18
         mean rate = 0.01 calls/second
     1-minute rate = 0.01 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.01 calls/second
               min = 11274.66 milliseconds
               max = 27216.13 milliseconds
              mean = 20036.82 milliseconds
            stddev = 4309.02 milliseconds
            median = 20936.50 milliseconds
              75% <= 23109.48 milliseconds
              95% <= 27216.13 milliseconds
              98% <= 27216.13 milliseconds
              99% <= 27216.13 milliseconds
            99.9% <= 27216.13 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.loadCache.cloud.2.instances.duration
             count = 18
         mean rate = 0.01 calls/second
     1-minute rate = 0.01 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.02 calls/second
               min = 305.69 milliseconds
               max = 738.70 milliseconds
              mean = 521.88 milliseconds
            stddev = 141.24 milliseconds
            median = 613.40 milliseconds
              75% <= 614.09 milliseconds
              95% <= 738.70 milliseconds
              98% <= 738.70 milliseconds
              99% <= 738.70 milliseconds
            99.9% <= 738.70 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.loadCache.cloud.3.instances.duration
             count = 18
         mean rate = 0.01 calls/second
     1-minute rate = 0.01 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.02 calls/second
               min = 3153.41 milliseconds
               max = 4785.20 milliseconds
              mean = 3737.11 milliseconds
            stddev = 425.92 milliseconds
            median = 3695.19 milliseconds
              75% <= 3933.13 milliseconds
              95% <= 4785.20 milliseconds
              98% <= 4785.20 milliseconds
              99% <= 4785.20 milliseconds
            99.9% <= 4785.20 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.loadCache.cloud.4.instances.duration
             count = 19
         mean rate = 0.01 calls/second
     1-minute rate = 0.02 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.02 calls/second
               min = 320.20 milliseconds
               max = 973.06 milliseconds
              mean = 586.41 milliseconds
            stddev = 191.79 milliseconds
            median = 611.98 milliseconds
              75% <= 717.29 milliseconds
              95% <= 973.06 milliseconds
              98% <= 973.06 milliseconds
              99% <= 973.06 milliseconds
            99.9% <= 973.06 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.loadCache.cloud.5.instances.duration
             count = 18
         mean rate = 0.01 calls/second
     1-minute rate = 0.01 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.02 calls/second
               min = 320.05 milliseconds
               max = 1149.56 milliseconds
              mean = 623.13 milliseconds
            stddev = 213.16 milliseconds
            median = 612.89 milliseconds
              75% <= 648.55 milliseconds
              95% <= 1149.56 milliseconds
              98% <= 1149.56 milliseconds
              99% <= 1149.56 milliseconds
            99.9% <= 1149.56 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.loadCache.cloud.6.instances.duration
             count = 18
         mean rate = 0.01 calls/second
     1-minute rate = 0.01 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.02 calls/second
               min = 308.39 milliseconds
               max = 970.46 milliseconds
              mean = 550.77 milliseconds
            stddev = 173.09 milliseconds
            median = 606.69 milliseconds
              75% <= 614.54 milliseconds
              95% <= 970.46 milliseconds
              98% <= 970.46 milliseconds
              99% <= 970.46 milliseconds
            99.9% <= 970.46 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.loadCache.cloud.7.instances.duration
             count = 18
         mean rate = 0.01 calls/second
     1-minute rate = 0.01 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.02 calls/second
               min = 317.27 milliseconds
               max = 715.16 milliseconds
              mean = 553.12 milliseconds
            stddev = 119.48 milliseconds
            median = 614.22 milliseconds
              75% <= 614.75 milliseconds
              95% <= 715.16 milliseconds
              98% <= 715.16 milliseconds
              99% <= 715.16 milliseconds
            99.9% <= 715.16 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.loadCache.cloud.8.instances.duration
             count = 18
         mean rate = 0.01 calls/second
     1-minute rate = 0.01 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.02 calls/second
               min = 317.33 milliseconds
               max = 1228.66 milliseconds
              mean = 608.88 milliseconds
            stddev = 215.26 milliseconds
            median = 614.49 milliseconds
              75% <= 618.79 milliseconds
              95% <= 1228.66 milliseconds
              98% <= 1228.66 milliseconds
              99% <= 1228.66 milliseconds
            99.9% <= 1228.66 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.loadCache.duration
             count = 18
         mean rate = 0.01 calls/second
     1-minute rate = 0.01 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.01 calls/second
               min = 43981.64 milliseconds
               max = 60016.49 milliseconds
              mean = 51572.46 milliseconds
            stddev = 4828.03 milliseconds
            median = 52476.96 milliseconds
              75% <= 54984.33 milliseconds
              95% <= 60016.49 milliseconds
              98% <= 60016.49 milliseconds
              99% <= 60016.49 milliseconds
            99.9% <= 60016.49 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.loadCache.instance.duration
             count = 266
         mean rate = 0.12 calls/second
     1-minute rate = 0.18 calls/second
     5-minute rate = 0.10 calls/second
    15-minute rate = 0.09 calls/second
               min = 548.33 milliseconds
               max = 4047.54 milliseconds
              mean = 951.78 milliseconds
            stddev = 331.68 milliseconds
            median = 898.55 milliseconds
              75% <= 1036.08 milliseconds
              95% <= 1500.39 milliseconds
              98% <= 1869.80 milliseconds
              99% <= 2080.83 milliseconds
            99.9% <= 4047.54 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.loadCache.server_array.duration
             count = 18
         mean rate = 0.01 calls/second
     1-minute rate = 0.01 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.02 calls/second
               min = 760.28 milliseconds
               max = 1804.78 milliseconds
              mean = 1083.02 milliseconds
            stddev = 284.29 milliseconds
            median = 963.96 milliseconds
              75% <= 1227.93 milliseconds
              95% <= 1804.78 milliseconds
              98% <= 1804.78 milliseconds
              99% <= 1804.78 milliseconds
            99.9% <= 1804.78 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.populateLinkedResources.duration
             count = 36
         mean rate = 0.02 calls/second
     1-minute rate = 0.03 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.05 calls/second
               min = 2.86 milliseconds
               max = 24.83 milliseconds
              mean = 4.48 milliseconds
            stddev = 3.56 milliseconds
            median = 3.79 milliseconds
              75% <= 4.61 milliseconds
              95% <= 8.09 milliseconds
              98% <= 24.83 milliseconds
              99% <= 24.83 milliseconds
            99.9% <= 24.83 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.refresh.duration
             count = 18
         mean rate = 0.01 calls/second
     1-minute rate = 0.01 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.01 calls/second
               min = 43990.15 milliseconds
               max = 60029.05 milliseconds
              mean = 51585.44 milliseconds
            stddev = 4830.74 milliseconds
            median = 52486.97 milliseconds
              75% <= 55006.76 milliseconds
              95% <= 60029.05 milliseconds
              98% <= 60029.05 milliseconds
              99% <= 60029.05 milliseconds
            99.9% <= 60029.05 milliseconds
```