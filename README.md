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



### Secondary resources and priming

Rightscale instances are linked to by a number of common resources.
This basic data is used to prime the cache and load additional resources.

The cache is bootstrapped by getting:
* Clouds: Instances will be found in each Cloud.

Addititional resources are retrieved if the Cloud supports them:

* Deployments
* Images
* ServerTemplates
* Datacenters
* InstanceTypes
* SshKeys
* Subnets

Because much of this data changes infrequently, it is only loaded once. Restart the plugin,
 or change its configuration to force a reload of the cache.

### Primary resources

At every refresh, load the cache with

* Instances
* Tags
* Inputs
* Servers
* ServerArrays

This data is considered more apt to change (new instances added, new tags defined)
and is therefore requested at each refresh interval.

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
* `RightscaleAPIRequest.request.error`: Count of API requests that resulted in errors.
* `RightscaleAPIRequest.request.fail`: Count of failed API requests.
* `RightscaleAPIRequest.request.success`: Count of successful API requests.
* `RightscaleAPIRequest.request.duration`: Timer for API requests.
* `RightscaleAPIRequest.request.count`: Total count of API requests.
* `RightscaleAPIRequest.request.skipped`: Count of times refresh was skipped because it was already running.
* `RightscaleNodes.refresh.rate`: Rate of Number of nodes / Refresh time.
* `RightscaleNodes.populateServerArrayNodes.duration`: Time taken to generate Nodes for the Instances in the ServerArrays
* `RightscaleNodes.populateServerNodes.duration`: Time taken to generate Nodes for the Instances in the Servers
* `RightscaleNodes.populateInstanceResources.duration`: Time taken to populate a single Node from linked Instance data.
* `RightscaleNodes.CacheLoader.load.duration`: Time taken to make API requests and load results in cache.
* `RightscaleNodes.CacheLoader.priming.duration`: Time taken to load primary resources into the cache.
* `RightscaleNodes.refresh.duration`: Time to refresh the cache and generate Nodes (milliseconds)

File a Github Issue if you have ideas about other useful metrics.

Example metrics output.

```
12/30/13 10:10:06 AM ===========================================================

-- Gauges ----------------------------------------------------------------------
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.nodes.count
             value = 4
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.since.lastUpdate
             value = 5

-- Counters --------------------------------------------------------------------
com.simplifyops.rundeck.plugin.resources.RightscaleAPIRequest.authentication
             count = 1
com.simplifyops.rundeck.plugin.resources.RightscaleAPIRequest.request.errors
             count = 0
com.simplifyops.rundeck.plugin.resources.RightscaleAPIRequest.request.fail
             count = 0
com.simplifyops.rundeck.plugin.resources.RightscaleAPIRequest.request.success
             count = 189
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.refresh.request.count
             count = 2
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.refresh.request.skipped
             count = 2

-- Meters ----------------------------------------------------------------------
com.simplifyops.rundeck.plugin.resources.CacheLoader.cache.rate
             count = 199187
         mean rate = 195.28 events/second
     1-minute rate = 2.21 events/second
     5-minute rate = 106.07 events/second
    15-minute rate = 112.96 events/second
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.refresh.rate
             count = 11
         mean rate = 0.01 events/second
     1-minute rate = 0.01 events/second
     5-minute rate = 0.01 events/second
    15-minute rate = 0.07 events/second

-- Timers ----------------------------------------------------------------------
com.simplifyops.rundeck.plugin.resources.CacheLoader.instance.duration
             count = 12
         mean rate = 0.01 calls/second
     1-minute rate = 0.03 calls/second
     5-minute rate = 0.04 calls/second
    15-minute rate = 0.27 calls/second
               min = 537.71 milliseconds
               max = 1070.56 milliseconds
              mean = 745.03 milliseconds
            stddev = 187.39 milliseconds
            median = 695.13 milliseconds
              75% <= 941.19 milliseconds
              95% <= 1070.56 milliseconds
              98% <= 1070.56 milliseconds
              99% <= 1070.56 milliseconds
            99.9% <= 1070.56 milliseconds
com.simplifyops.rundeck.plugin.resources.CacheLoader.load.duration
             count = 2
         mean rate = 0.00 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.00 calls/second
    15-minute rate = 0.00 calls/second
               min = 136055.60 milliseconds
               max = 144409.06 milliseconds
              mean = 140232.33 milliseconds
            stddev = 5906.79 milliseconds
            median = 140232.33 milliseconds
              75% <= 144409.06 milliseconds
              95% <= 144409.06 milliseconds
              98% <= 144409.06 milliseconds
              99% <= 144409.06 milliseconds
            99.9% <= 144409.06 milliseconds
com.simplifyops.rundeck.plugin.resources.CacheLoader.priming.duration
             count = 2
         mean rate = 0.00 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.00 calls/second
    15-minute rate = 0.00 calls/second
               min = 104142.92 milliseconds
               max = 113552.26 milliseconds
              mean = 108847.59 milliseconds
            stddev = 6653.40 milliseconds
            median = 108847.59 milliseconds
              75% <= 113552.26 milliseconds
              95% <= 113552.26 milliseconds
              98% <= 113552.26 milliseconds
              99% <= 113552.26 milliseconds
            99.9% <= 113552.26 milliseconds
com.simplifyops.rundeck.plugin.resources.CacheLoader.server_array.duration
             count = 3
         mean rate = 0.00 calls/second
     1-minute rate = 0.01 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.07 calls/second
               min = 847.85 milliseconds
               max = 1228.39 milliseconds
              mean = 1087.83 milliseconds
            stddev = 208.85 milliseconds
            median = 1187.26 milliseconds
              75% <= 1228.39 milliseconds
              95% <= 1228.39 milliseconds
              98% <= 1228.39 milliseconds
              99% <= 1228.39 milliseconds
            99.9% <= 1228.39 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleAPIRequest.request.duration
             count = 216
         mean rate = 0.21 calls/second
     1-minute rate = 0.58 calls/second
     5-minute rate = 0.29 calls/second
    15-minute rate = 0.53 calls/second
               min = 149.41 milliseconds
               max = 110740.62 milliseconds
              mean = 6052.95 milliseconds
            stddev = 17643.47 milliseconds
            median = 944.63 milliseconds
              75% <= 1340.09 milliseconds
              95% <= 48440.38 milliseconds
              98% <= 81799.41 milliseconds
              99% <= 99335.35 milliseconds
            99.9% <= 110740.62 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.populateInstanceResources.duration
             count = 8
         mean rate = 0.01 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.05 calls/second
    15-minute rate = 0.31 calls/second
               min = 8.35 milliseconds
               max = 32.35 milliseconds
              mean = 12.24 milliseconds
            stddev = 8.18 milliseconds
            median = 9.24 milliseconds
              75% <= 10.87 milliseconds
              95% <= 32.35 milliseconds
              98% <= 32.35 milliseconds
              99% <= 32.35 milliseconds
            99.9% <= 32.35 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.populateServerArrayNodes.duration
             count = 2
         mean rate = 0.00 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.08 calls/second
               min = 10.11 milliseconds
               max = 20.70 milliseconds
              mean = 15.40 milliseconds
            stddev = 7.49 milliseconds
            median = 15.40 milliseconds
              75% <= 20.70 milliseconds
              95% <= 20.70 milliseconds
              98% <= 20.70 milliseconds
              99% <= 20.70 milliseconds
            99.9% <= 20.70 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.populateServerNodes.duration
             count = 2
         mean rate = 0.00 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.08 calls/second
               min = 28.04 milliseconds
               max = 71.49 milliseconds
              mean = 49.77 milliseconds
            stddev = 30.73 milliseconds
            median = 49.77 milliseconds
              75% <= 71.49 milliseconds
              95% <= 71.49 milliseconds
              98% <= 71.49 milliseconds
              99% <= 71.49 milliseconds
            99.9% <= 71.49 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.refresh.duration
             count = 2
         mean rate = 0.00 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.00 calls/second
    15-minute rate = 0.00 calls/second
               min = 136162.22 milliseconds
               max = 144448.19 milliseconds
              mean = 140305.20 milliseconds
            stddev = 5859.06 milliseconds
            median = 140305.20 milliseconds
              75% <= 144448.19 milliseconds
              95% <= 144448.19 milliseconds
              98% <= 144448.19 milliseconds
              99% <= 144448.19 milliseconds
            99.9% <= 144448.19 milliseconds
```