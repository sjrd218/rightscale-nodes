# Rightscale Nodes Plugin

Generates Rundeck Nodes from the servers and server_arrays in your RightScale account.

**Note**: Requires Rundeck 2.0

## Build

    ./gradlew clean build

## Install

Copy the `rightscale-nodes-x.y.jar` file to the `libext/` directory inside your Rundeck installation.


## Configuration

* `endpoint`: RightScale  API Endpoint URL. Must support API v1.5
* `email`: Email address for RightScale User
* `password`: Password for RightScale User
* `refreshInterval`: Minimum time in seconds between API requests to RightScale (default is 60)
* `username`: Default SSH username for remote execution

