# Rightscale Nodes Plugin

Generates Rundeck Nodes from the operational Instances associated
with the Servers and ServerArrays in your RightScale account.

Much of the data seen on the "Info" tab in the Rightscale dashboard becomes available as Node attributes and tags.
The plugin uses the Rightscale 1.5 API to retrieve the necessary resources to populate the Rundeck Nodes
(see [API reference](http://reference.rightscale.com/api1.5/index.html),
[API guide](http://support.rightscale.com/12-Guides/RightScale_API_1.5)).

To make the plugin more efficient and not place undue demands on the Rightscale API server,
the plugin caches data and refreshes the data at a configured time interval.

**Note**: Requires Rundeck 2.0+

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
* `Account`: String, The Rightscale Account number.
* `Endpoint`: String, RightScale  API Endpoint URL. Must support API v1.5 (default is https://us-3.rightscale.com).
* `Refresh Interval`: Integer, Minimum time in seconds between API requests to RightScale (default is 180).
* `Load all resources` (default false): If checked true, all resource data is requested.
Includes loading resources for Datacenters, Deployments, Images, InstanceTypes, SshKeys, Subnets, ServerTemplates.

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
Between refresh intervals, nodes from the last refresh are returned.


### Primary and secondary resource caching

Rightscale instances are linked to by a number of common resources.
This basic data is used to prime the cache and load additional resources.

### Minimal resources

At every refresh, load the cache with
* Clouds
* Instances
* Tags
* Inputs
* Servers
* ServerArrays

This data is considered more apt to change (new instances added, new tags defined)
and is therefore requested at each refresh interval.

### Full load of resources

Additional resources are retrieved if the Cloud supports them:

* Deployments
* Images
* ServerTemplates
* Datacenters
* InstanceTypes
* SshKeys
* Subnets

Because much of this data changes infrequently, it is only loaded once. Restart the plugin,
or change its configuration to force a reload of the cache.

## Jobs

Two example jobs are included for checking the refresh cycle. These jobs are useful if
you need the plugin to run at a regular interval, not just refreshing through normal use.
This might be important if refreshes must be done at a predictable cycle.

The job definitions can be found in src/main/jobs:

* check-refresh: Runs every 5 minutes and checks the data in cache to determine the last refresh time.
* trigger-refresh: Is invoked by check-refresh's error handler if the check fails and triggers a refresh.

Customize these jobs to suit your environment or particular needs.

Requirements: The check-refresh job depends on:

* xmlstarlet to parse the xml cache file
* gnu date to convert date/time format into unix epoch time in seconds

Caveats:

The jobs rely on a couple of side effects which might not exist in future versions of rundeck.

* The "kick" behavior depends on rundeck causing a full refresh from the plugin when the config file changes.
* The log output from the trigger-refresh job ref call contains stdout that comes from server log.

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

The plugin uses the [codahale Metrics library](http://metrics.codahale.com/) to keep a number of statistics
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
1/2/14 9:21:04 AM ==============================================================

-- Gauges ----------------------------------------------------------------------
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.nodes.count
             value = 2
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.refresh.last.ago
             value = 1
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.refresh.last.duration
             value = 23764

-- Counters --------------------------------------------------------------------
com.simplifyops.rundeck.plugin.resources.RightscaleAPIRequest.authentication
             count = 1
com.simplifyops.rundeck.plugin.resources.RightscaleAPIRequest.request.errors
             count = 0
com.simplifyops.rundeck.plugin.resources.RightscaleAPIRequest.request.fail
             count = 0
com.simplifyops.rundeck.plugin.resources.RightscaleAPIRequest.request.success
             count = 260
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.refresh.request.count
             count = 12
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.refresh.request.skipped
             count = 12

-- Meters ----------------------------------------------------------------------
com.simplifyops.rundeck.plugin.resources.CacheLoader.cache.rate
             count = 1197997
         mean rate = 399.33 events/second
     1-minute rate = 45.33 events/second
     5-minute rate = 296.51 events/second
    15-minute rate = 359.47 events/second
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.refresh.rate
             count = 37
         mean rate = 0.01 events/second
     1-minute rate = 0.02 events/second
     5-minute rate = 0.01 events/second
    15-minute rate = 0.02 events/second

-- Timers ----------------------------------------------------------------------
com.simplifyops.rundeck.plugin.resources.CacheLoader.instance.duration
             count = 24
         mean rate = 0.01 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.02 calls/second
               min = 545.99 milliseconds
               max = 1374.15 milliseconds
              mean = 754.20 milliseconds
            stddev = 215.48 milliseconds
            median = 661.94 milliseconds
              75% <= 915.33 milliseconds
              95% <= 1288.94 milliseconds
              98% <= 1374.15 milliseconds
              99% <= 1374.15 milliseconds
            99.9% <= 1374.15 milliseconds
com.simplifyops.rundeck.plugin.resources.CacheLoader.load.duration
             count = 12
         mean rate = 0.00 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.00 calls/second
    15-minute rate = 0.00 calls/second
               min = 23758.11 milliseconds
               max = 138028.05 milliseconds
              mean = 37740.25 milliseconds
            stddev = 31739.25 milliseconds
            median = 30420.58 milliseconds
              75% <= 31909.38 milliseconds
              95% <= 138028.05 milliseconds
              98% <= 138028.05 milliseconds
              99% <= 138028.05 milliseconds
            99.9% <= 138028.05 milliseconds
com.simplifyops.rundeck.plugin.resources.CacheLoader.primary.duration
             count = 12
         mean rate = 0.00 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.00 calls/second
    15-minute rate = 0.00 calls/second
               min = 23757.50 milliseconds
               max = 32965.22 milliseconds
              mean = 28675.09 milliseconds
            stddev = 3156.08 milliseconds
            median = 29590.35 milliseconds
              75% <= 31313.16 milliseconds
              95% <= 32965.22 milliseconds
              98% <= 32965.22 milliseconds
              99% <= 32965.22 milliseconds
            99.9% <= 32965.22 milliseconds
com.simplifyops.rundeck.plugin.resources.CacheLoader.secondary.duration
             count = 1
         mean rate = 0.00 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.00 calls/second
    15-minute rate = 0.00 calls/second
               min = 108769.11 milliseconds
               max = 108769.11 milliseconds
              mean = 108769.11 milliseconds
            stddev = 0.00 milliseconds
            median = 108769.11 milliseconds
              75% <= 108769.11 milliseconds
              95% <= 108769.11 milliseconds
              98% <= 108769.11 milliseconds
              99% <= 108769.11 milliseconds
            99.9% <= 108769.11 milliseconds
com.simplifyops.rundeck.plugin.resources.CacheLoader.server_array.instances.duration
             count = 24
         mean rate = 0.01 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.02 calls/second
               min = 840.48 milliseconds
               max = 1710.80 milliseconds
              mean = 1181.28 milliseconds
            stddev = 221.98 milliseconds
            median = 1175.49 milliseconds
              75% <= 1333.08 milliseconds
              95% <= 1645.79 milliseconds
              98% <= 1710.80 milliseconds
              99% <= 1710.80 milliseconds
            99.9% <= 1710.80 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleAPIRequest.request.duration
             count = 289
         mean rate = 0.10 calls/second
     1-minute rate = 0.12 calls/second
     5-minute rate = 0.08 calls/second
    15-minute rate = 0.14 calls/second
               min = 156.14 milliseconds
               max = 105855.92 milliseconds
              mean = 3302.36 milliseconds
            stddev = 9988.62 milliseconds
            median = 740.77 milliseconds
              75% <= 1207.59 milliseconds
              95% <= 22502.56 milliseconds
              98% <= 36556.01 milliseconds
              99% <= 61067.81 milliseconds
            99.9% <= 105855.92 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.populateInstanceResources.duration
             count = 24
         mean rate = 0.01 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.01 calls/second
    15-minute rate = 0.02 calls/second
               min = 1.93 milliseconds
               max = 27.85 milliseconds
              mean = 3.43 milliseconds
            stddev = 5.22 milliseconds
            median = 2.30 milliseconds
              75% <= 2.58 milliseconds
              95% <= 21.93 milliseconds
              98% <= 27.85 milliseconds
              99% <= 27.85 milliseconds
            99.9% <= 27.85 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.populateServerArrayNodes.duration
             count = 12
         mean rate = 0.00 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.00 calls/second
    15-minute rate = 0.01 calls/second
               min = 5.59 milliseconds
               max = 57.69 milliseconds
              mean = 10.70 milliseconds
            stddev = 14.82 milliseconds
            median = 6.33 milliseconds
              75% <= 6.80 milliseconds
              95% <= 57.69 milliseconds
              98% <= 57.69 milliseconds
              99% <= 57.69 milliseconds
            99.9% <= 57.69 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.populateServerNodes.duration
             count = 12
         mean rate = 0.00 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.00 calls/second
    15-minute rate = 0.01 calls/second
               min = 0.16 milliseconds
               max = 6.82 milliseconds
              mean = 0.76 milliseconds
            stddev = 1.91 milliseconds
            median = 0.19 milliseconds
              75% <= 0.23 milliseconds
              95% <= 6.82 milliseconds
              98% <= 6.82 milliseconds
              99% <= 6.82 milliseconds
            99.9% <= 6.82 milliseconds
com.simplifyops.rundeck.plugin.resources.RightscaleNodes.refresh.duration
             count = 12
         mean rate = 0.00 calls/second
     1-minute rate = 0.00 calls/second
     5-minute rate = 0.00 calls/second
    15-minute rate = 0.00 calls/second
               min = 23764.28 milliseconds
               max = 138099.83 milliseconds
              mean = 37752.82 milliseconds
            stddev = 31757.83 milliseconds
            median = 30428.69 milliseconds
              75% <= 31916.74 milliseconds
              95% <= 138099.83 milliseconds
              98% <= 138099.83 milliseconds
              99% <= 138099.83 milliseconds
            99.9% <= 138099.83 milliseconds
```