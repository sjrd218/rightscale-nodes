package com.simplifyops.rundeck.plugin.resources

import com.codahale.metrics.ConsoleReporter
import com.codahale.metrics.Gauge
import com.codahale.metrics.Meter
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.resources.ResourceModelSource
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException
import com.codahale.metrics.MetricRegistry

import org.apache.log4j.Logger

import java.util.concurrent.TimeUnit

public class RightscaleNodes implements ResourceModelSource {
    static Logger logger = Logger.getLogger(RightscaleNodes.class);

    /**
     * Configuration parameters.
     */
    private Properties configuration;

    /**
     * The nodeset filled by the query result.
     */
    private INodeSet nodeset;

    private RightscaleAPI query;

    private RightscaleCache cache;

    private boolean initialized = false

    private Thread refreshThread
    private boolean cachePrimed = false

    private MetricRegistry metrics = RightscaleNodesFactory.metrics

    /**
     * Default constructor used by RightscaleNodesFactory. Uses RightscaleAPIRequest for querying.
     * @param configuration Properties containing plugin configuration values.
     */
    public RightscaleNodes(Properties configuration) {
        this(configuration, new RightscaleAPIRequest(configuration))
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

        this.configuration = configuration
        this.query = api
        this.cache = cache

        logger.debug("DEBUG: New RightscaleNodes object created.")
    }

    /**
     * validate required configuration params are valid. Used by the factory.
     * @throws ConfigurationException
     */
    void validate() throws ConfigurationException {
        if (null == configuration.getProperty(RightscaleNodesFactory.EMAIL)) {
            throw new ConfigurationException("email is required");
        }
        if (null == configuration.getProperty(RightscaleNodesFactory.PASSWORD)) {
            throw new ConfigurationException("password is required");
        }
        if (null == configuration.getProperty(RightscaleNodesFactory.ACCOUNT)) {
            throw new ConfigurationException("account is required");
        }
        if (null == configuration.getProperty(RightscaleNodesFactory.REFRESH_INTERVAL)) {
            throw new ConfigurationException("interval is required");
        }
        final String interval = configuration.getProperty(RightscaleNodesFactory.REFRESH_INTERVAL)
        try {
            Integer.parseInt(interval);
        } catch (NumberFormatException e) {
            throw new ConfigurationException(RightscaleNodesFactory.REFRESH_INTERVAL + " value is not valid: " + interval);
        }
        if (null == configuration.getProperty(RightscaleNodesFactory.INPUT_PATT)) {
            throw new ConfigurationException("inputs is required");
        }
        final String timeout = configuration.getProperty(RightscaleNodesFactory.HTTP_TIMEOUT)
        if (null != timeout) {
            try {
                Integer.parseInt(timeout);
            } catch (NumberFormatException e) {
                throw new ConfigurationException(RightscaleNodesFactory.HTTP_TIMEOUT + " value is not valid: " + timeout);
            }
        }
        final String metricsInterval = configuration.getProperty(RightscaleNodesFactory.METRICS_INTVERVAL)
        if (null != metricsInterval) {
            try {
                Integer.parseInt(metricsInterval);
            } catch (NumberFormatException e) {
                throw new ConfigurationException(RightscaleNodesFactory.METRICS_INTVERVAL + " value is not valid: " + metricsInterval);
            }
        }
    }
    /**
     * Initialize the cache, query and metrics objects.
     */
    void initialize() {
        if (!initialized) {
            // Cache
            int refreshSecs = Integer.parseInt(configuration.getProperty(RightscaleNodesFactory.REFRESH_INTERVAL));
            this.cache.setRefreshInterval(refreshSecs * 1000)
            this.cache.initialize()

            // Query
            this.query.initialize()

            // Metrics
            if (!metrics.getGauges().containsKey(MetricRegistry.name(RightscaleNodes.class, "nodes.count"))) {
                metrics.register(MetricRegistry.name(RightscaleNodes.class, "nodes.count"), new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return (null == nodeset) ? 0 : nodeset.getNodes().size()
                    }
                });
            }

            if (configuration.containsKey(RightscaleNodesFactory.METRICS_INTVERVAL)) {
                final ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                        .convertRatesTo(TimeUnit.SECONDS)
                        .convertDurationsTo(TimeUnit.MILLISECONDS)
                        .build();
                reporter.start(Integer.parseInt(configuration.getProperty(RightscaleNodesFactory.METRICS_INTVERVAL)), TimeUnit.MINUTES);
            }

            // done.
            initialized = true
        }
    }

    /**
     * Populate the cache from query data.
     */
    void loadCache() {
        def timer = metrics.timer(MetricRegistry.name(RightscaleNodes, 'loadCache.duration')).time()

        def long starttime = System.currentTimeMillis()
        logger.debug("loadCache() started.")
        System.out.println("DEBUG: loadCache() started.")

        if (!cachePrimed) {
            logger.debug("loadCache() Priming cache")
            System.out.println("DEBUG: loadCache() Priming cache")

            cache.updateClouds(query.getClouds())
            cache.updateDeployments(query.getDeployments())
            cache.updateServerTemplates(query.getServerTemplates())

            def clouds = cache.getClouds().values()
            clouds.each { cloud ->
                def cloud_id = cloud.getId()
                System.out.println("DEBUG: Loading cache with resources for cloud: ${cloud.attributes['name']}")
                logger.debug("Loading cache with resources for cloud: ${cloud.attributes['name']}")
                cache.updateDatacenters(query.getDatacenters(cloud_id))
                cache.updateInstanceTypes(query.getInstanceTypes(cloud_id))
                cache.updateSubnets(query.getSubnets(cloud_id))
            }
            cachePrimed = true;
            logger.debug("loadCache() cache prime complete")
            System.out.println("DEBUG: loadCache() cache prime complete")
        }

        /**
         * Get the Instances and their Images, Tags and Inputs
         */
        def clouds = cache.getClouds().values()
        clouds.each { cloud ->
            def cloud_id = cloud.getId()
            cache.updateSshKeys(query.getSshKeys(cloud_id))

            def cloudInstancesTimer = metrics.timer(MetricRegistry.name(RightscaleNodes, "loadCache.cloud.${cloud_id}.instances.duration")).time()
            cache.updateInstances(query.getInstances(cloud_id))
            cloudInstancesTimer.stop()

            // Filter on instances that are in the 'operational' state.
            def instances = cache.getInstances(cloud_id).values().findAll {
                "operational".equalsIgnoreCase(it.attributes['state'])
            }
            System.out.println("DEBUG: Found ${instances.size()} operational instances.")
            logger.debug("Found ${instances.size()} operational instances.")

            /**
             * Post process the Instances to gather other linked resources. Only instances in the operational state are proessed..
             *  - Images: It's assumed that instances share a small set of base images. Get only the ones in use.
             *  - Tags: Tags aren't referenced as a link and must be searched for.
             *  - Inputs: Resource Inputs must also be searched for.
             */
            Map<String, RightscaleResource> images = [:]
            Map<String, RightscaleResource> tags = [:]
            def instanceTimer = metrics.timer(MetricRegistry.name(RightscaleNodes, 'loadCache.instance.duration'))

            instances.each { instance ->
                def t = instanceTimer.time()
                System.out.println("DEBUG: Retreiving image, inputs and tags for instance: ${instance.attributes['name']}.")
                logger.debug("Retreiving image, inputs and tags for instance: ${instance.attributes['name']}.")
                // Get the Image. Skip it if we already have it, otherwise query for it.
                if (!cache.getImage(instance.links['image'])) {
                    images.put(instance.links['image'], query.getImage(instance.links['image']))
                }

                // Get the Inputs and update the cache with them.
                cache.updateInputs(query.getInputs(instance.links['inputs']))
                // Get the Tags.
                def linkedTags = query.getTags(instance.links['self']).values()
                System.out.println("DEBUG: Retreived ${linkedTags.size()} tags for this instance.")
                linkedTags.each { tag ->
                    System.out.println("DEBUG: Caching tags: \"" + tag.attributes['tags'] + "\" for instance: " + instance.attributes['name'])
                    tags.put(instance.links['self'], tag)
                }
                t.stop()
            }
            cache.updateImages(images)
            cache.updateTags(tags)
        }
        /**
         * Get the Servers and ServerArrays
         */
        cache.updateServers(query.getServers())
        cache.updateServerArrays(query.getServerArrays())
        def serverArrays = cache.getServerArrays().values()
        System.out.println("DEBUG: Retreiving instances for ${serverArrays.size()} server arrays.")
        logger.debug("Retreiving instances for ${serverArrays.size()} server arrays.")
        def severArrayTimer = metrics.timer(MetricRegistry.name(RightscaleNodes, 'loadCache.server_array.duration'))

        serverArrays.each {
            System.out.println("DEBUG: Retreiving instances for server array: ${it.attributes['name']}.")
            logger.debug("Retreiving instances for server array: ${it.attributes['name']}.")
            def t = severArrayTimer.time()
            def server_array_id = it.getId()
            cache.updateServerArrayInstances(query.getServerArrayInstances(server_array_id))
            t.stop()
        }

        def endtime = System.currentTimeMillis()
        def duration = (endtime - starttime)
        System.out.println("DEBUG: loadCache() completed. (resources=${cache.size()}, duration=${duration})")
        logger.debug("loadCache() completed. (resources=${cache.size()}, duration=${duration})")
        timer.stop()
    }

    /**
     * Query RightScale for their instances and return them as Nodes.
     */
    @Override
    public synchronized INodeSet getNodes() throws ResourceModelSourceException {

        // Initialize query and cache instances in case this is the first time through.
        initialize()

        /**
         * Haven't got any nodes yet so get them synchronously.
         */
        if (null == nodeset) {
            System.println("DEBUG: Empty nodeset. Refreshing nodes.")
            logger.debug("Empty nodeset. Refreshing nodes.")
            nodeset = refresh()

        } else {

            if (!needsRefresh()) {
                System.println("DEBUG: Nodes don't need a refresh.")
                logger.debug("Nodes don't need a refresh.")
                return nodeset;
            }

            /**
             * Query asynchronously.
             */
            logger.debug "Nodes need refresh."
            System.out.println("DEBUG: Nodes need refresh.")
            if (!asyncRefreshRunning()) {
                refreshThread = Thread.start { nodeset = refresh() }
                logger.debug("Running refresh in background thread")
                System.out.println("DEBUG: Running refresh in background thread")
            } else {
                logger.debug("Refresh thread already running. (thread id:" + refreshThread.id + ")")
                System.out.println("DEBUG: Refresh thread already running. (thread id:" + refreshThread.id + ")")
                return nodeset
            }
        }

        /**
         * Return the nodeset
         */
        return nodeset;
    }

    /**
     * Returns true if the last refresh time was longer ago than the refresh interval.
     */
    boolean needsRefresh() {
        return cache.needsRefresh()
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
        def long starttime = System.currentTimeMillis()
        def timer = metrics.timer(MetricRegistry.name(RightscaleNodes.class, 'refresh.duration')).time()
        final Meter refreshRate = metrics.meter(MetricRegistry.name(RightscaleNodes.class, "refresh", "rate"))
        refreshRate.mark()
        System.out.println("DEBUG: refresh() started.")
        logger.debug("DEBUG: refresh() started.")

        // load up the cache.
        try {
            loadCache()
        } catch (Exception e) {
            throw new ResourceModelSourceException("Error while loading cache.", e)
        }

        /**
         * Generate the nodes.
         */
        INodeSet nodes = new NodeSetImpl();
        // Generate Nodes from Instances launched by Servers.
        nodes.putNodes(getServerNodes(cache))
        // Generate Nodes from Instances launched by ServerArrays.
        nodes.putNodes(getServerArrayNodes(cache))

        def duration = (System.currentTimeMillis() - starttime)
        System.println("DEBUG: refresh() ended. (nodes=${nodes.getNodes().size()}, duration=${duration})")
        logger.debug("refresh() ended. (nodes=${nodes.getNodes().size()}, duration=${duration})")

        timer.stop()
        refreshRate.mark(nodes.getNodes().size())
        return nodes;
    }

    /**
     * Generate Nodes from Instances launched by Servers.
     * @param api the RightscaleQuery
     * @return a node set of Nodes
     */
    INodeSet getServerNodes(RightscaleAPI api) {
        def long starttime = System.currentTimeMillis()
        def timer = metrics.timer(MetricRegistry.name(RightscaleNodes, 'getServerNodes.duration')).time()

        /**
         * Create a node set from the result.
         */
        def nodeset = new NodeSetImpl();
        /**
         * List the Servers
         */
        def servers = api.getServers().values()

        // Only process operational servers with a current instance.
        def operationalServers = servers.findAll { "operational".equals(it.attributes['state']) && it.links.containsKey('current_instance') }
        logger.debug("Retrieved ${servers.size()} servers in operational state.")
        System.out.println("DEBUG: Retrieved ${servers.size()} servers in operational state.")

        operationalServers.each { server ->
            logger.debug("Retreiving current_instance for server: ${server.attributes['name']}")
            /**
             * Get the cloud so we lookup the instance.
             */
            //def cloud_href = server.links['cloud']
            def cloud_href = server.cloud
            if (null == cloud_href) {
                logger.error("Could not determine the cloud for this server: ${server}. ")
                throw new ResourceModelSourceException("cloud link not found for server: " + server.attributes['name'])
            }
            def cloud_id = cloud_href.split("/").last()
            logger.debug("Retrieving current_instance: " + server.links['current_instance'])
            System.out.println("DEBUG: Retrieving current_instance: " + server.links['current_instance'])
            def InstanceResource instance = api.getInstances(cloud_id).get(server.links['current_instance'])
            if (null == instance) {
                logger.error("Failed getting instance for server: ${server.links['self']}. current_instance: "
                        + server.links['current_instance'])
                throw new ResourceModelSourceException(
                        "Failed getting instance for server " + server.links['self'] + ". current_instance: "
                                + server.links['current_instance']
                                + "instances:" + api.getInstances(cloud_id))
            }
            // Extra precaution: only process instances that are also in state, operational.
            if ("operational".equalsIgnoreCase(instance.attributes['state'])) {
                System.out.println("DEBUG: Creating node for server current_instance: ${instance.attributes['name']}")
                logger.debug("Creating node for server current_instance: ${instance.attributes['name']}")
                def NodeEntryImpl newNode = createNode(server.attributes['name'])

                server.populate(newNode)

                instance.populate(newNode)

                populateLinkedResources(api, instance, newNode)

                // Add the node to the result.
                nodeset.putNode(newNode)
                logger.debug("Added node: " + newNode.getNodename() + " for server: ${server.links['self']}")
                System.out.println("DEBUG: Added node: " + newNode.getNodename() + " for server: ${server.links['self']}")
            }

        }
        def duration = (System.currentTimeMillis() - starttime)
        System.println("DEBUG: getServerNodes() ended. (nodes=${nodeset.getNodes().size()}, duration=${duration})")
        logger.debug("getServerNodes() ended. (nodes=${nodeset.getNodes().size()}, duration=${duration})")
        timer.stop()

        return nodeset
    }

    /**
     * Generate Nodes from Instances launched by ServerArrays.
     * @param api the RightscaleQuery
     * @return a new INodeSet of nodes for each instance in the query result.
     */
    INodeSet getServerArrayNodes(RightscaleAPI api) {
        def long starttime = System.currentTimeMillis()
        def timer = metrics.timer(MetricRegistry.name(RightscaleNodes, 'getServerArrayNodes.duration')).time()

        /**
         * Create a nodeset for query the result.
         */
        def nodeset = new NodeSetImpl();
        /**
         * List the ServerArrays
         */
        def serverArrays = api.getServerArrays().values()
        System.out.println("DEBUG: Iterating over ${serverArrays.size()} server arrays")
        logger.debug("Retrieved ${serverArrays.size()} server arrays")
        serverArrays.each { serverArray ->
            def server_array_id = serverArray.getId()
            logger.debug("Retrieving instances for array: " + serverArray.attributes['name'])
            /**
             * Get the Instances for this array
             */
            def instances = api.getServerArrayInstances(server_array_id)
            // Only include instances that are operational.
            def operationalInstances = instances.values().findAll { "operational".equalsIgnoreCase(it.attributes['state']) }
            logger.debug("Retrieved ${operationalInstances.size()} operational instances.")
            System.out.println("DEBUG: Retrieved ${operationalInstances.size()} operational instances.")

            operationalInstances.each { instance ->
                /**
                 * Populate the Node entry with the instance data.
                 */
                System.out.println("DEBUG: Creating node for instance: ${instance.attributes['name']}")
                logger.debug("Creating node for ${serverArray.attributes['name']} server array instance: ${instance.attributes['name']}")

                def NodeEntryImpl newNode = createNode(instance.attributes['name'])

                instance.populate(newNode)
                newNode.setNodename(instance.attributes['name'])  // TODO: Convention agreement.

                populateLinkedResources(api, instance, newNode)

                serverArray.populate(newNode)

                nodeset.putNode(newNode)
                System.out.println("DEBUG: Added ${serverArray.attributes['name']} server array instance: ${instance.attributes['name']}")
                logger.debug("Added ${serverArray.attributes['name']} server array instance: ${instance.attributes['name']}")
            }
        }
        def duration = (System.currentTimeMillis() - starttime)
        System.println("DEBUG: getServerArrayNodes() ended. (nodes=${nodeset.getNodes().size()}, duration=${duration})")
        logger.debug("getServerArrayNodes() ended. (nodes=${nodeset.getNodes().size()}, duration=${duration})")
        timer.stop()

        return nodeset;
    }

    /**
     * Convenience method to create a new Node with defaults.
     * @param name Name of the node
     * @return a new Node
     */
    NodeEntryImpl createNode(final String name) {
        NodeEntryImpl newNode = new NodeEntryImpl(name);
        newNode.setUsername(configuration.getProperty(RightscaleNodesFactory.USERNAME))
        // Based on convention.
        newNode.setOsName("Linux");   // - Hard coded default.
        newNode.setOsFamily("unix");  // - "
        newNode.setOsArch("x86_64");  // - "
        return newNode
    }

    /**
     *
     * @param api
     * @param instance
     * @param newNode
     */
    void populateLinkedResources(RightscaleAPI api, InstanceResource instance, NodeEntryImpl newNode) {
        def long starttime = System.currentTimeMillis()
        def timer = metrics.timer(MetricRegistry.name(RightscaleNodes, 'populateLinkedResources.duration')).time()

        def cloud_id = instance.links['cloud'].split("/").last()

        instance.links.each { rel, href ->
            switch (rel) {
                case "cloud":
                    def cloud = api.getClouds().get(instance.links['cloud'])
                    cloud.populate(newNode)
                    break
                case "datacenter":
                    def datacenter = api.getDatacenters(cloud_id).get(instance.links['datacenter'])
                    datacenter.populate(newNode)
                    break
                case "deployment":
                    def deployment = api.getDeployments().get(instance.links['deployment'])
                    deployment.populate(newNode)
                    break
                case "image":
                    def image = api.getImages(cloud_id).get(instance.links['image'])
                    image.populate(newNode)
                    break
                case "inputs":
                    def inputs = api.getInputs(instance.links['inputs'])
                    inputs.values().each {
                        if (it.attributes['name'].matches(configuration.getProperty(RightscaleNodesFactory.INPUT_PATT))) {
                            it.populate(newNode)
                            logger.debug("Setting node attribute for input: ${it.attributes['name']}")
                            System.out.println("DEBUG: Setting node attribute for input: ${it.attributes['name']}")
                        } else {
                            logger.debug("Ignored input ${it.attributes['name']}. Did not match: " + configuration.getProperty(RightscaleNodesFactory.INPUT_PATT))
                            System.out.println("DEBUG: Ignored input ${it.attributes['name']}. Did not match: " + configuration.getProperty(RightscaleNodesFactory.INPUT_PATT))
                        }
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
        System.out.println("DEBUG: retrieving tags for instance: " + instance.links['self'])
        logger.debug("retrieving tags for instance: " + instance.links['self'])
        def tags = api.getTags(instance.links['self'])
        tags.values().each { TagsResource tag ->
            tag.attributes['tags'].split(",").each { name ->

                if (name.matches(configuration.getProperty(RightscaleNodesFactory.TAG_PATT))) {

                    /**
                     * Experiment: Generate an attribute if the tag contains an equal sign.
                     */
                    if (Boolean.parseBoolean(configuration.getProperty(RightscaleNodesFactory.TAG_ATTR)) && tag.hasAttributeForm(name)) {
                        System.out.println("DEBUG: mapping tag to node attribute: ${name}.")
                        logger.debug("mapping tag to node attribute: ${name}.")
                        tag.setAttribute(name, newNode)
                    } else {
                        RightscaleResource.setTag(name, newNode)
                        logger.debug("setting tag: ${name}")
                        System.out.println("DEBUG: setting tag: ${name}")
                    }

                } else {
                    logger.debug("Ignoring tag ${name}. Did not match ${configuration.getProperty(RightscaleNodesFactory.TAG_PATT)}")
                    System.out.println("DEBUG: Ignoring tag ${name}. Did not match ${configuration.getProperty(RightscaleNodesFactory.TAG_PATT)}")
                }
            }
        }
        def duration = (System.currentTimeMillis() - starttime)
        System.println("DEBUG: populateLinkedResources() ended. (duration=${duration})")
        logger.debug("populateLinkedResources() ended. (duration=${duration})")
        timer.stop()
    }
}
