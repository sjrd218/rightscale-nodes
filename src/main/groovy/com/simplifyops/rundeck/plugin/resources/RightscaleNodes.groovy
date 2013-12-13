package com.simplifyops.rundeck.plugin.resources

import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.resources.ResourceModelSource
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException
import groovyx.gpars.GParsPool
import org.apache.log4j.Logger

public class RightscaleNodes implements ResourceModelSource {
    static Logger logger = Logger.getLogger(RightscaleNodes.class);

    /**
     * Configuration parameters.
     */
    private String email;
    private String password;
    private String account;
    private long refreshInterval;
    private String endpoint;
    private String username;

    /**
     * Time nodes were last updated.
     */
    private long lastRefresh = 0;
    /**
     * The nodeset filled by the query result.
     */
    private INodeSet nodeset;

    private RightscaleAPI query;

    private RightscaleCache cache;

    private boolean initialized = false

    private Thread refreshThread

    /**
     * Default constructor used by RightscaleNodesFactory. Uses RightscaleAPIRequest for querying.
     * @param configuration Properties containing plugin configuration values.
     */
    public RightscaleNodes(Properties configuration) {
        this(configuration,
                new RightscaleAPIRequest(
                        configuration.getProperty(RightscaleNodesFactory.EMAIL),
                        configuration.getProperty(RightscaleNodesFactory.PASSWORD),
                        configuration.getProperty(RightscaleNodesFactory.ACCOUNT),
                        configuration.getProperty(RightscaleNodesFactory.ENDPOINT),
                )
        )
    }

    /**
     * Base constructor.
     * @param configuration Properties containing plugin configuration values.
     * @param query Rightscale API client used to query resources.
     */
    public RightscaleNodes(Properties configuration, RightscaleAPI query) {
        this(configuration, query, new RightscaleBasicCache())
    }

    /**
     * Base constructor.
     * @param configuration Properties containing plugin configuration values.
     * @param api Rightscale API client used to query resources.
     */
    public RightscaleNodes(Properties configuration, RightscaleAPI api, RightscaleCache cache) {

        email = configuration.getProperty(RightscaleNodesFactory.EMAIL)
        password = configuration.getProperty(RightscaleNodesFactory.PASSWORD)
        account = configuration.getProperty(RightscaleNodesFactory.ACCOUNT)
        endpoint = configuration.getProperty(RightscaleNodesFactory.ENDPOINT)
        username = configuration.getProperty(RightscaleNodesFactory.USERNAME)

        int refreshSecs = 30;
        final String refreshStr = configuration.getProperty(RightscaleNodesFactory.REFRESH_INTERVAL)
        if (null != refreshStr && !"".equals(refreshStr)) {
            try {
                refreshSecs = Integer.parseInt(refreshStr);
            } catch (NumberFormatException e) {
                logger.warn(RightscaleNodesFactory.REFRESH_INTERVAL + " value is not valid: " + refreshStr);
            }
        }
        refreshInterval = refreshSecs * 1000;
        this.query = api
        this.cache = cache

        println("DEBUG: New RightscaleNodes object created.")
    }
    /**
     * validate required params are set. Used by the factory.
     * @throws ConfigurationException
     */
    void validate() throws ConfigurationException {
        if (null == email) {
            throw new ConfigurationException("email is required");
        }
        if (null == password) {
            throw new ConfigurationException("password is required");
        }
        if (null == account) {
            throw new ConfigurationException("account is required");
        }
    }

    void initialize() {
        if(!initialized){
            this.query.initialize()
            this.cache.initialize()
            initialized = true
        }
    }

    /**
     * Populate the cache.
     */
    void loadCache() {
        def long starttime = System.currentTimeMillis()
        GParsPool.withPool {
            GParsPool.executeAsyncAndWait(
                    { cache.updateClouds(query.getClouds()) },
                    { cache.updateDeployments(query.getDeployments()) },
                    { cache.updateServers(query.getServers()) },
                    { cache.updateServerTemplates(query.getServerTemplates()) },
                    { cache.updateServerArrays(query.getServerArrays()) }
            )
        }

        def clouds = cache.getClouds().values()
        clouds.each { cloud ->
            def cloud_id = cloud.getId()
            GParsPool.withPool {
                GParsPool.executeAsyncAndWait(
                        { cache.updateDatacenters(query.getDatacenters(cloud_id)) },
                        { cache.updateInstances(query.getInstances(cloud_id)) },
                        { cache.updateInstanceTypes(query.getInstanceTypes(cloud_id)) },
                        { cache.updateSubnets(query.getSubnets(cloud_id)) },
                        { cache.updateSshKeys(query.getSshKeys(cloud_id)) }
                )
            }

            /**
             * Get each of the images individually to avoid making long query requests.
             */
            Map<String, RightscaleResource> images = [:]
            cache.getInstances(cloud_id).values().each {
                images.put(it.links['image'], query.getImage(it.links['image']))
            }
            cache.updateImages(images)
        }

        cache.getServerArrays().values().each {
            def server_array_id = it.getId()
            cache.updateServerArrayInstances(query.getServerArrayInstances(server_array_id))
        }
        System.println("DEBUG: loadCache() time: " + (System.currentTimeMillis() - starttime))
    }

    /**
     * Query RightScale for their instances and return them as Nodes.
     */
    @Override
    public synchronized INodeSet getNodes() throws ResourceModelSourceException {
        def long starttime = System.currentTimeMillis()
        /**
         * Haven't got any nodes yet so get them synchronously.
         */
        if (null == nodeset) {
            System.println("DEBUG: Getting nodes synchronously first time.")
            nodeset = refresh()

        } else {

            if (!needsRefresh()) {
                System.println("DEBUG: Nodes don't need a refresh.")
                return nodeset;
            }

            /**
             * Query asynchronously.
             */
            System.println("DEBUG: Asynchronously getting nodes.")
            if (!asyncRefreshRunning()) {
                refreshThread = Thread.start { nodeset = refresh() }
            }
        }

        System.println("DEBUG: getNodes() time: " + (System.currentTimeMillis() - starttime))
        /**
         * Return the nodeset
         */
        return nodeset;
    }

    /**
     * Returns true if the last refresh time was longer ago than the refresh interval.
     */
     boolean needsRefresh() {
        return refreshInterval < 0 || (System.currentTimeMillis() - lastRefresh > refreshInterval);
    }

    private boolean asyncRefreshRunning() {
         return null != refreshThread && refreshThread.isAlive()
    }
    /**
     * Query the RightScale API for instances and map them to Nodes.
     *
     * @return nodeset of Nodes
     */
    INodeSet refresh() {
        initialize()
        /**
         * Load up the cache.
         */
        loadCache()

        /**
         * Generate the nodes.
         */
        INodeSet nodes = new NodeSetImpl();
        nodes.putNodes(getServerNodes(cache))
        nodes.putNodes(getServerArrayNodes(cache))

        lastRefresh = System.currentTimeMillis();

        return nodes;
    }

    /**
     * Query all servers and get their Instances as Nodes
     * @param api the RightscaleQuery
     * @return a node set of Nodes
     */
    INodeSet getServerNodes(RightscaleAPI api) {
        /**
         * Create a node set from the result.
         */
        def nodeset = new NodeSetImpl();
        /**
         * List the Servers
         */
        def servers = api.getServers().values()
        System.out.println("DEBUG: Iterating over ${servers.size()} servers")

        servers.each { server ->
            // Only process servers with a current instance. TODO: Use api filter to limit results.
            if (server.links.containsKey('current_instance')) {
                def NodeEntryImpl newNode = createNode(server.attributes['name'])
                server.populate(newNode)
                server.links.each { rel, href ->
                    switch (rel) {
                        case "deployment":
                            def deployment = api.getDeployments().get(server.links['deployment'])
                            deployment.populate(newNode)
                            break
                    }
                }
                def cloud_href = server.links['cloud']
                if (null == cloud_href) {
                    throw new ResourceModelSourceException("cloud link not found for server: " + server.attributes['name'])
                }
                def cloud_id = cloud_href.split("/").last()
                def InstanceResource instance = api.getInstances(cloud_id).get(server.links['current_instance'])
                instance.populate(newNode)

                populateLinkedResources(api, instance, newNode)

                // Add the node to the result.
                nodeset.putNode(newNode)
            }
        }
        return nodeset
    }

    /**
     * Make nodes from ServerArray instances.
     * @param api the RightscaleQuery
     * @return a new INodeSet of nodes for each instance in the query result.
     */
    INodeSet getServerArrayNodes(RightscaleAPI api) {
        /**
         * Create a nodeset for query the result.
         */
        def nodeset = new NodeSetImpl();
        /**
         * List the ServerArrays
         */
        def serverArrays = api.getServerArrays().values()
        System.out.println("DEBUG: Iterating over ${serverArrays.size()} server arrays")
        serverArrays.each { serverArray ->
            def server_array_id = serverArray.getId()
            /**
             * Get the Instances for this array
             */
            def instances = api.getServerArrayInstances(server_array_id)
            instances.values().each { instance ->
                /**
                 * Populate the Node entry with the instance data.
                 */
                def NodeEntryImpl newNode = createNode(instance.attributes['name'])

                instance.populate(newNode)
                newNode.setNodename(instance.attributes['name'])

                populateLinkedResources(api, instance, newNode)
                serverArray.populate(newNode)

                nodeset.putNode(newNode)
                System.out.println("DEBUG: Added server array instance over ${instance.attributes['name']}")
            }
        }
        return nodeset;
    }

    /**
     * Convenience method to create a new Node with defaults.
     * @param name Name of the node
     * @return a new Node
     */
    NodeEntryImpl createNode(final String name) {
        NodeEntryImpl newNode = new NodeEntryImpl(name);
        newNode.setUsername(username) // - Config property.
        newNode.setOsName("Linux");   // - Hard coded default.
        newNode.setOsFamily("unix");  // - "
        newNode.setOsArch("x86_64");  // - "
        return newNode
    }


    void populateLinkedResources(RightscaleAPI api, InstanceResource instance, NodeEntryImpl newNode) {
        def cloud_id = instance.links['cloud'].split("/").last()

        instance.links.each { rel, href ->
            switch (rel) {
                case "self":
                    def tags = api.getTags(instance.links['self'])
                    tags.values().each {
                        it.populate(newNode)
                    }
                    break
                case "cloud":
                    def cloud = api.getClouds().get(instance.links['cloud'])
                    cloud.populate(newNode)
                    break
                case "image":
                    def image = api.getImages(cloud_id).get(instance.links['image'])
                    image.populate(newNode)
                    break
                case "inputs":
                    def instance_id = instance.links['self'].split("/").last()
                    def inputs = api.getInputs(cloud_id, instance_id)
                    inputs.values().each {
                        it.populate(newNode)
                    }
                    break;
                case "instance_type":
                    def instance_type = api.getInstanceTypes(cloud_id).get(instance.links['instance_type'])
                    instance_type.populate(newNode)
                    break
                case "server_template":
                    def server_template = api.getServerTemplates().get(instance.links['server_template'])
                    server_template.populate(newNode)
                    break
                case "ssh_key":
                    def ssh_key = api.getSshKeys(cloud_id).get(instance.links['ssh_key'])
                    ssh_key.populate(newNode)
                    break
            }
        }
    }

}
