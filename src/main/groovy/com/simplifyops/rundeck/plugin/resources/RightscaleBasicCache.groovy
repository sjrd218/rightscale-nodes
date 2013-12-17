package com.simplifyops.rundeck.plugin.resources

import org.apache.log4j.Logger

/**
 *
 */
class RightscaleBasicCache implements RightscaleCache {
    static Logger logger = Logger.getLogger(RightscaleBasicCache.class);

    private Map<String, CachedResourceCollection> resources

    private int refreshSecs = 30;
    private long refreshInterval;
    private long lastRefresh = 0;


    public RightscaleBasicCache(int refreshSecs) {
        this.refreshSecs = refreshSecs
        refreshInterval = refreshSecs * 1000;

        resources = [:]
        resources['cloud'] = new CachedResourceCollection()
        resources['datacenter'] = new CachedResourceCollection()
        resources['deployment'] = new CachedResourceCollection()
        resources['image'] = new CachedResourceCollection()
        resources['instance_type'] = new CachedResourceCollection()
        resources['instance'] = new CachedResourceCollection()
        resources['inputs'] = new CachedResourceCollection()
        resources['server_array'] = new CachedResourceCollection()
        resources['server_array_instance'] = new CachedResourceCollection()
        resources['server_template'] = new CachedResourceCollection()
        resources['server'] = new CachedResourceCollection()
        resources['ssh_key'] = new CachedResourceCollection()
        resources['subnet'] = new CachedResourceCollection()
        resources['tag'] = new CachedResourceCollection()

    }

    /**
     * Defaults to 30 sec refresh interval.
     */
    public RightscaleBasicCache() {
        this(30)
    }


    @Override
    public void initialize() {
        // nothing to do
    }

    @Override
    public void setRefreshInterval(int millisecs) {
        refreshInterval = millisecs
    }

    /**
     * Load the cache
     */
    public void load(Closure builder) {
        builder()
    }

    @Override
    public boolean needsRefresh() {
        return refreshInterval <= 0 || ((System.currentTimeMillis() - lastRefresh) > refreshInterval);
    }

    @Override
    public void clear() {
        resources.values().each {
            it.clear()
        }
    }
    /**
     * Generic method for storing resources.
     * Also sets the lastRefresh and ctime for the collection.
     * @param key The key for the type of resource (eg, 'cloud')
     * @param resources The map of RightscaleResources
     */
    private void storeResources(String key, Map<String, RightscaleResource> resources) {
        this.resources[key].putAll(resources)
        def now = System.currentTimeMillis()
        this.resources[key].ctime = now
        lastRefresh = now
    }

    /**
     * Generic method for getting resources.
     * @param key The key for the type of resource (eg, 'cloud')
     * @param resources The map of RightscaleResources
     */
    private Map<String, RightscaleResource> getResources(String key) {
        resources[key].atime = System.currentTimeMillis()
        return resources[key].toMap()
    }

    @Override
    Map<String, RightscaleResource> getClouds() {
        return getResources('cloud')
    }

    @Override
    void updateClouds(Map<String, RightscaleResource> clouds) {
        storeResources('cloud', clouds)
    }

    @Override
    Map<String, RightscaleResource> getDatacenters(String cloud_id) {
        return getResources('datacenter')
    }

    @Override
    void updateDatacenters(Map<String, RightscaleResource> datacenters) {
        storeResources('datacenter', datacenters)
    }

    Map<String, RightscaleResource> getDatacenters() {
        return getResources('datacenter')
    }

    @Override
    Map<String, RightscaleResource> getDeployments() {
        return getResources('deployment')
    }

    @Override
    void updateDeployments(Map<String, RightscaleResource> deployments) {
        storeResources('deployment', deployments)
    }

    @Override
    Map<String, RightscaleResource> getImages(String cloud_id) {
        return getResources('image')
    }

    @Override
    RightscaleResource getImage(String href) {
        return getResources('image').get(href)
    }

    Map<String, RightscaleResource> getImages() {
        return getResources('image')
    }

    @Override
    void updateImages(Map<String, RightscaleResource> images) {
        storeResources('image', images)
    }

    @Override
    Map<String, RightscaleResource> getInputs(String cloud_id, String instance_id) {
        return getResources('inputs')
    }

    Map<String, RightscaleResource> getInputs() {
        return getResources('inputs')
    }

    @Override
    Map<String, RightscaleResource> getInputs(String href) {
        return getResources('inputs')
    }

    @Override
    void updateInputs(Map<String, RightscaleResource> inputs) {
        storeResources('inputs', inputs)
    }

    @Override
    Map<String, RightscaleResource> getInstances(String cloud_id) {
        return getResources('instance')
    }

    Map<String, RightscaleResource> getInstances() {
        return getResources('instance')
    }

    @Override
    void updateInstances(Map<String, RightscaleResource> instances) {
        storeResources('instance', instances)
    }

    @Override
    Map<String, RightscaleResource> getInstanceTypes(String cloud_id) {
        return getResources('instance_type')
    }

    @Override
    void updateInstanceTypes(Map<String, RightscaleResource> instanceTypes) {
        storeResources('instance_type', instanceTypes)
    }

    @Override
    Map<String, RightscaleResource> getServerArrays() {
        return getResources('server_array')
    }

    @Override
    void updateServerArrays(Map<String, RightscaleResource> serverArrays) {
        storeResources('server_array', serverArrays)
    }

    @Override
    public Map<String, RightscaleResource> getServerArrayInstances(String server_array_id) {
        return getResources('server_array_instance')
    }

    @Override
    void updateServerArrayInstances(Map<String, RightscaleResource> instances) {
        storeResources('server_array_instance', instances)
    }

    @Override
    Map<String, RightscaleResource> getServers() {
        return getResources('server')
    }

    @Override
    void updateServers(Map<String, RightscaleResource> servers) {
        storeResources('server', servers)
    }

    @Override
    Map<String, RightscaleResource> getServerTemplates() {
        return getResources('server_template')
    }

    @Override
    void updateServerTemplates(Map<String, RightscaleResource> serverTemplates) {
        storeResources('server_template', serverTemplates)
    }

    @Override
    Map<String, RightscaleResource> getSubnets(String cloud_id) {
        return getResources('subnet')
    }

    @Override
    void updateSubnets(Map<String, RightscaleResource> subnets) {
        storeResources('subnet', subnets)
    }

    @Override
    Map<String, RightscaleResource> getSshKeys(String cloud_id) {
        return getResources('ssh_key')
    }

    @Override
    void updateSshKeys(Map<String, RightscaleResource> ssh_keys) {
        storeResources('ssh_key', ssh_keys)
    }

    @Override
    Map<String, RightscaleResource> getTags(String href) {
        return getResources('tag')
    }

    @Override
    void updateTags(Map<String, RightscaleResource> tags) {
        storeResources('tag', tags)
    }


    class CachedResourceCollection {
        private long ctime
        private long atime
        private Map<String, RightscaleResource> collection;
        private long refreshInterval;

        CachedResourceCollection() {
            setInterval(30 * 1000)
            collection = [:]
        }

        void setInterval(int millis) {
            refreshInterval=millis
        }

        public Map<String, RightscaleResource> toMap() {
            atime = System.currentTimeMillis()
            return collection
        }

        public RightscaleResource get(String key) {
            atime = System.currentTimeMillis()
            return collection.get(key)
        }

        boolean exists(String key) {
            return collection.containsKey(key)
        }


        public boolean needsRefresh() {
            return refreshInterval <= 0 || ((System.currentTimeMillis() - ctime) > refreshInterval);
        }


        void putAll(Map<String, RightscaleResource> map) {
            collection.putAll(map)
            ctime = System.currentTimeMillis()
        }

        void remove(String key) {
            collection.remove(key)
            ctime = System.currentTimeMillis()
        }

        int size() {
            return collection.size()
        }

        void clear() {
            collection.clear()
        }
    }

}


