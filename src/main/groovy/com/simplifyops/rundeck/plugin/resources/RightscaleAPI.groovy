package com.simplifyops.rundeck.plugin.resources;


/**
 * Abstracts access to the resources provided by the Rightscale v1.5 API
 */

public interface RightscaleAPI {

    void initialize();
    Map<String, RightscaleResource> getClouds();
    Map<String, RightscaleResource> getDatacenters(String cloud_id);
    Map<String, RightscaleResource> getDeployments();
    Map<String, RightscaleResource> getImages(String cloud_id);
    RightscaleResource getImage(String href);
    Map<String, RightscaleResource> getInputs(String href);
    Map<String, RightscaleResource> getInstances(String cloud_id);
    Map<String, RightscaleResource> getInstanceTypes(String cloud_id);
    Map<String, RightscaleResource> getServerArrays();
    Map<String, RightscaleResource> getServerArrayInstances(String server_array_id);
    Map<String, RightscaleResource> getServers();
    Map<String, RightscaleResource> getServerTemplates();
    Map<String, RightscaleResource> getSubnets(String cloud_id);
    Map<String, RightscaleResource> getSshKeys(String cloud_id);
    Map<String, RightscaleResource> getTags(String href);

}
