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
    public int size() {
        return resources.inject(0) { count, k, v -> count + v.size() }
    }

    @Override
    public long getLastRefresh() {
        return lastRefresh
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
        System.out.println("DEBUG: Cached ${resources.size()} ${key} resources.")
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
    public boolean hasResource(String key, String href) {
        return resources[key].collection.containsKey(href)
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
        def resources = [:]
        String cloud_href = "/api/clouds/"+cloud_id
        getResources('datacenter').values().findAll {cloud_href.equals(it.links['cloud'])}.each {
            resources.put(it.links['self'],it)
        }

        return resources
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
        def resources = [:]
        String cloud_href = "/api/clouds/"+cloud_id
        getResources('image').values().findAll {cloud_href.equals(it.links['cloud'])}.each {
            resources.put(it.links['self'],it)
        }

        return resources
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
    void clearInputs() {
        resources['inputs'].clear()
    }

    Map<String, RightscaleResource> getInputs() {
        return getResources('inputs')
    }

    /**
     * Get the Inputs that have the specified parent link relationship.
     * @param href the parent href. eg /api/clouds/366783326/instances/ABC135655295DEF/inputs
     * @return
     */
    @Override
    Map<String, RightscaleResource> getInputs(String href) {
        if (null == href) {
            println("DEBUG: getInputs() no href specified so returning all inputs.")
            return getResources('inputs')
        }
        def Map<String, RightscaleResource> inputs = [:]
        getResources('inputs').values().findAll {href.equals(it.links['parent'])}.each {
            inputs.put(it.links['self'], it)
        }
        return inputs
    }

    @Override
    void updateInputs(Map<String, RightscaleResource> inputs) {
        storeResources('inputs', inputs)
    }

    @Override
    void clearInstances() {
        resources['instance'].clear()
    }


    @Override
    void clearInstances(String cloud_id) {
        String cloud_href = "/api/clouds/"+cloud_id
        def matches = getResources('instance').values().findAll {cloud_href.equals(it.links['cloud'])}
        matches.each {
            resources['instance'].remove(it.links['self'])
        }
    }

    @Override
    Map<String, RightscaleResource> getInstances(String cloud_id) {
        def instances = [:]
        String cloud_href = "/api/clouds/"+cloud_id
        getResources('instance').values().findAll {cloud_href.equals(it.links['cloud'])}.each {
            instances.put(it.links['self'],it)
        }

        return instances
    }


    @Override
    Map<String, RightscaleResource> getInstances() {
        return getResources('instance')
    }

    @Override
    void updateInstances(Map<String, RightscaleResource> instances) {
        storeResources('instance', instances)
    }

    @Override
    Map<String, RightscaleResource> getInstanceTypes(String cloud_id) {
        def resources = [:]
        String cloud_href = "/api/clouds/"+cloud_id
        getResources('instance_type').values().findAll {cloud_href.equals(it.links['cloud'])}.each {
            resources.put(it.links['self'],it)
        }

        return resources
    }

    @Override
    void updateInstanceTypes(Map<String, RightscaleResource> instanceTypes) {
        storeResources('instance_type', instanceTypes)
    }

    @Override
    void clearServerArrayInstances() {
        resources['server_array_instance'].clear()
    }

    @Override
    void clearServerArrayInstances(String server_array_id) {
        def instances = getServerArrayInstances(server_array_id).values()
        instances.each {
            resources['server_array_instance'].remove(it.links['self'])
        }
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
    void clearServers() {
        resources['server'].clear()
    }

    @Override
    public Map<String, RightscaleResource> getServerArrayInstances(String server_array_id) {
        def parent_href = "/api/server_arrays/" + server_array_id
        Map<String, RightscaleResource> instances = [:]
        def matched = getResources('server_array_instance').values().findAll {
            parent_href.equals(it.links['parent'])
        }
        matched.each {
            instances.put(it.links['self'], it)
        }
        return instances
    }

    @Override
    void updateServerArrayInstances(Map<String, RightscaleResource> instances) {
        storeResources('server_array_instance', instances)
    }

    @Override
    void clearServerArrays() {
        resources['server_array'].clear()
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
        def resources = [:]
        String cloud_href = "/api/clouds/"+cloud_id
        getResources('ssh_key').values().findAll {cloud_href.equals(it.links['cloud'])}.each {
            resources.put(it.links['self'],it)
        }

        return resources
    }

    @Override
    void updateSshKeys(Map<String, RightscaleResource> ssh_keys) {
        storeResources('ssh_key', ssh_keys)
    }



    @Override
    Map<String, RightscaleResource> getTags(String href) {
        def Map<String, RightscaleResource> tags = [:]
        if (null == href) {
            println("DEBUG: getTags() No href specified so returning all tags")
            return getResources('tag')
        }
        if (getResources('tag').containsKey(href)) {
            def tag = getResources('tag').get(href)
            tags.put(href, tag)
        }
        return tags
    }

    @Override
    void updateTags(Map<String, RightscaleResource> tags) {
        storeResources('tag', tags)
    }

    @Override
    void clearTags() {
        resources['tag'].clear()
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
            refreshInterval = millis
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


        synchronized void putAll(Map<String, RightscaleResource> map) {
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


