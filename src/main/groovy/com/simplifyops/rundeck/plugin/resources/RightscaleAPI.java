package com.simplifyops.rundeck.plugin.resources;

import java.util.Map;

/**
 * Abstracts access to the resources provided by the Rightscale v1.5 API
 */

public interface RightscaleAPI {

    Map<String, RightscaleResource> getClouds();
    Map<String, RightscaleResource> getDatacenters(String cloud_id);
    Map<String, RightscaleResource> getDeployments();
    Map<String, RightscaleResource> getImages(String cloud_id);
    Map<String, RightscaleResource> getInputs(String cloud_id, String instance_id);
    Map<String, RightscaleResource> getInstances(String cloud_id);
    Map<String, RightscaleResource> getInstanceTypes(String cloud_id);
    Map<String, RightscaleResource> getServerArrays();
    Map<String, RightscaleResource> getServers();
    Map<String, RightscaleResource> getServerTemplates();
    Map<String, RightscaleResource> getSubnets(String cloud_id);
    Map<String, RightscaleResource> getTags(String href);
    Map<String, RightscaleResource> getResources(String resourceType);

}
