package com.simplifyops.rundeck.plugin.resources

import org.apache.log4j.Logger

/**
 *
 */
class RightscaleCache implements RightscaleAPI {
    static Logger logger = Logger.getLogger(RightscaleCache.class);

    private Map<String, CachedResourceCollection> resources

    public RightscaleCache() {
        resources = [:]
        resources['cloud'] = new CachedResourceCollection()
        resources['datacenter'] = new CachedResourceCollection()
        resources['deployment'] = new CachedResourceCollection()
        resources['image'] = new CachedResourceCollection()
        resources['instance_type'] = new CachedResourceCollection()
        resources['instance'] = new CachedResourceCollection()
        resources['inputs'] = new CachedResourceCollection()
        resources['server_array'] = new CachedResourceCollection()
        resources['server_template'] = new CachedResourceCollection()
        resources['server'] = new CachedResourceCollection()
        resources['subnet'] = new CachedResourceCollection()
        resources['tag'] = new CachedResourceCollection()
    }

    /**
     * Load the cache
     */
    public void load(Closure builder) {
        builder()
    }

    public boolean needsRefresh() {
        resources.values().each {
            if (it.needsRefresh()) return true
        }
        return false
    }

    public void cleanUp() {
        resources.values().each {
            it.cleanUp()
        }
    }

    Map<String, CachedResourceCollection> getResources(String type) {
        if (!resources.containsKey(type)) {
            //throw new RuntimeException("cache does not contain resources for type: " + type)
            return [:]
        } else {
            return resources[type].toMap()
        }
    }

    @Override
    Map<String, RightscaleResource> getClouds() {
        return getResources('cloud')
    }

    void updateClouds(Map<String, RightscaleResource> clouds) {
        resources['cloud'].putAll(clouds)
    }

    @Override
    Map<String, RightscaleResource> getDatacenters(String cloud_id) {
        return resources['datacenter'].toMap()
    }

    void updateDatacenters(Map<String, RightscaleResource> datacenters) {
        resources['datacenter'].putAll(datacenters)
    }

    Map<String, RightscaleResource> getDatacenters() {
        return resources['datacenter'].toMap()
    }

    @Override
    Map<String, RightscaleResource> getDeployments() {
        return resources['deployment'].toMap()
    }

    void updateDeployments(Map<String, RightscaleResource> deployments) {
        resources['deployment'].putAll(deployments)
    }

    @Override
    Map<String, RightscaleResource> getImages(String cloud_id) {
        return resources['image'].toMap()
    }

    Map<String, RightscaleResource> getImages() {
        return resources['image'].toMap()
    }

    void updateImages(Map<String, RightscaleResource> images) {
        resources['image'].putAll(images)
    }

    @Override
    Map<String, RightscaleResource> getInputs(String cloud_id, String instance_id) {
        return resources['inputs'].toMap()
    }
    Map<String, RightscaleResource> getInputs() {
        return resources['inputs'].toMap()
    }

    void updateInputs(Map<String, RightscaleResource> inputs) {
        resources['inputs'].putAll(inputs)
    }

    @Override
    Map<String, RightscaleResource> getInstances(String cloud_id) {
        return resources['instance'].toMap()
    }

    Map<String, RightscaleResource> getInstances() {
        return resources['instance'].toMap()
    }

    void updateInstances(Map<String, RightscaleResource> instances) {
        resources['instance'].putAll(instances)
    }

    @Override
    Map<String, RightscaleResource> getInstanceTypes(String cloud_id) {
        return resources['instance_type'].toMap()
    }

    void updateInstanceTypes(Map<String, RightscaleResource> instanceTypes) {
        resources['instance_type'].putAll(instanceTypes)
    }

    @Override
    Map<String, RightscaleResource> getServerArrays() {
        return resources['server_array'].toMap()
    }

    void updateServerArrays(Map<String, RightscaleResource> serverArrays) {
        resources['server_array'].putAll(serverArrays)
    }

    @Override
    Map<String, RightscaleResource> getServers() {
        return resources['server'].toMap()
    }

    void updateServers(Map<String, RightscaleResource> servers) {
        resources['server'].putAll(servers)
    }

    @Override
    Map<String, RightscaleResource> getServerTemplates() {
        return resources['server_template'].toMap()
    }

    void updateServerTemplates(Map<String, RightscaleResource> serverTemplates) {
        resources['server_template'].putAll(serverTemplates)
    }

    @Override
    Map<String, RightscaleResource> getSubnets(String cloud_id) {
        return resources['subnet'].toMap()
    }

    void updateSubnets(Map<String, RightscaleResource> subnets) {
        resources['subnet'].putAll(subnets)
    }

    @Override
    Map<String, RightscaleResource> getTags(String href) {
        return resources['tag'].toMap()
    }

    void updateTags(Map<String, RightscaleResource> tags) {
        resources['tag'].putAll(tags)
    }


    class CachedResourceCollection {
        private long ctime
        private long atime

        private Map<String, RightscaleResource> collection;

        CachedResourceCollection() {
            collection = [:]
        }

        public Map<String, RightscaleResource> toMap() {
            atime = System.currentTimeMillis()
            return collection
        }

        public RightscaleResource get(String key) {
            collection.atime = System.currentTimeMillis()
            return collection.get(key)
        }

        public boolean needsRefresh() {
            if (collection.size()==0) return true
            return true
        }

        public void cleanUp() {
            def Iterator iter = collection.keySet().iterator()
            while(iter.hasNext()) {
                String href = iter.next()
                //evict(href)
            }
        }

        void putAll(Map<String, RightscaleResource> map) {
            collection.putAll(map)
            ctime = System.currentTimeMillis()
        }

        private void evict(String href) {
            collection.remove(href)
            ctime = System.currentTimeMillis()
        }
    }

}


