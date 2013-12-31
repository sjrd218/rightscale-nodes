package com.simplifyops.rundeck.plugin.resources

import com.codahale.metrics.ConsoleReporter
import com.codahale.metrics.Gauge
import com.codahale.metrics.Meter
import com.codahale.metrics.MetricRegistry
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.resources.ResourceModelSource
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException
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

    private CacheLoader loader;

    private boolean initialized = false

    private Thread refreshThread

    private long lastRefreshDuration = 0L

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

        this.loader = CacheLoader.create(CacheLoader.STRATEGY_V2)

        logger.info("DEBUG: New RightscaleNodes object created.")
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

            if (!metrics.getGauges().containsKey(MetricRegistry.name(RightscaleNodes.class, "since.lastUpdate"))) {
                metrics.register(MetricRegistry.name(RightscaleNodes.class, "since.lastUpdate"), new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return (null == nodeset) ? 0 : sinceLastRefresh()
                    }
                });
            }

            if (!metrics.getGauges().containsKey(MetricRegistry.name(RightscaleNodes.class, "refresh.last.duration"))) {
                metrics.register(MetricRegistry.name(RightscaleNodes.class, "refresh.last.duration"), new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return lastRefreshDuration
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
        loader.load(cache, query)
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
            logger.info("Empty nodeset. Refreshing nodes.")
            nodeset = refresh()

        } else {

            if (!needsRefresh()) {

                System.println("DEBUG: Nodes don't need a refresh.")
                logger.info("Nodes don't need a refresh.")

                return nodeset;
            }


            logger.info "Nodes need a refresh."
            System.out.println("DEBUG: Nodes need a refresh.")
            // Get a reading. how far behind our we on the refresh?
            sinceLastRefresh()
            /**
             * Query asynchronously.
             */

            if (!asyncRefreshRunning()) {
                refreshThread = Thread.start { nodeset = refresh() }
                logger.info("Running refresh in background thread")
                System.out.println("DEBUG: Running refresh in background thread")
            } else {
                logger.info("Refresh thread already running. (thread id:" + refreshThread.id + ")")
                System.out.println("DEBUG: Refresh thread already running. (thread id:" + refreshThread.id + ")")
                metrics.counter(MetricRegistry.name(RightscaleNodes.class, "refresh.request.skipped")).inc();

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

    private long sinceLastRefresh() {
        def lastRefresh = cache.getLastRefresh()
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastRefresh)
    }

    private final def refreshCount = metrics.counter(MetricRegistry.name(RightscaleNodes.class, "refresh.request.count"));
    private final Meter refreshRate = metrics.meter(MetricRegistry.name(RightscaleNodes.class, "refresh", "rate"))
    def refreshDuration = metrics.timer(MetricRegistry.name(RightscaleNodes.class, 'refresh.duration'))

    /**
     * Query the RightScale API for instances and map them to Nodes.
     *
     * @return nodeset of Nodes
     */
    INodeSet refresh() {
        def long starttime = System.currentTimeMillis()
        def refreshDuration = refreshDuration.time()
        refreshRate.mark()
        System.out.println("DEBUG: refresh() started.")
        logger.info("DEBUG: refresh() started.")

        // load up the cache.
        loadCache()

        /**
         * Generate the nodes.
         */
        INodeSet nodes = new NodeSetImpl();
        // Generate Nodes from Instances launched by Servers.
        nodes.putNodes(populateServerNodes(cache))
        // Generate Nodes from Instances launched by ServerArrays.
        nodes.putNodes(populateServerArrayNodes(cache))

        lastRefreshDuration = (System.currentTimeMillis() - starttime)
        System.println("DEBUG: refresh() ended. (nodes=${nodes.getNodes().size()}, duration=${lastRefreshDuration})")
        logger.info("refresh() ended. (nodes=${nodes.getNodes().size()}, duration=${lastRefreshDuration})")

        refreshDuration.stop()
        refreshRate.mark(nodes.getNodes().size())
        refreshCount.inc()

        return nodes;
    }

    /**
     * Generate Nodes from Instances launched by Servers.
     * @param api the RightscaleQuery
     * @return a node set of Nodes
     */
    INodeSet populateServerNodes(RightscaleAPI api) {
        def long starttime = System.currentTimeMillis()
        def timer = metrics.timer(MetricRegistry.name(RightscaleNodes, 'populateServerNodes.duration')).time()

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
        logger.info("Retrieved ${servers.size()} servers in operational state.")
        System.out.println("DEBUG: Retrieved ${servers.size()} servers in operational state.")

        operationalServers.each { server ->
            logger.info("Retreiving current_instance for server: ${server.attributes['name']}")
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
            logger.info("Retrieving current_instance: " + server.links['current_instance'])
            System.out.println("DEBUG: Retrieving current_instance: " + server.links['current_instance'])
            def InstanceResource instance = api.getInstances(cloud_id).get(server.links['current_instance'])
            if (null == instance) {
                logger.error("Failed getting instance for server: ${server.links['self']}. current_instance: "
                        + server.links['current_instance'])
                throw new ResourceModelSourceException(
                        "Failed getting instance for server " + server.links['self'] + ". current_instance: "
                                + server.links['current_instance'])
            }
            // Extra precaution: only process instances that are also in state, operational.
            if ("operational".equalsIgnoreCase(instance.attributes['state'])) {
                System.out.println("DEBUG: Creating node for server current_instance: ${instance.attributes['name']}")
                logger.info("Creating node for server current_instance: ${instance.attributes['name']}")
                def NodeEntryImpl newNode = createNode(server.attributes['name'])

                server.populate(newNode)

                instance.populate(newNode)

                populateInstanceResources(api, instance, newNode)

                // Add the node to the result.
                nodeset.putNode(newNode)
                logger.info("Added node: " + newNode.getNodename() + " for server: ${server.links['self']}")
                System.out.println("DEBUG: Added node: " + newNode.getNodename() + " for server: ${server.links['self']}")
            }

        }
        def duration = (System.currentTimeMillis() - starttime)
        System.println("DEBUG: populateServerNodes() ended. (nodes=${nodeset.getNodes().size()}, duration=${duration})")
        logger.info("populateServerNodes() ended. (nodes=${nodeset.getNodes().size()}, duration=${duration})")
        timer.stop()

        return nodeset
    }

    /**
     * Generate Nodes from Instances launched by ServerArrays.
     * @param api the RightscaleQuery
     * @return a new INodeSet of nodes for each instance in the query result.
     */
    INodeSet populateServerArrayNodes(RightscaleAPI api) {
        def long starttime = System.currentTimeMillis()
        def timer = metrics.timer(MetricRegistry.name(RightscaleNodes, 'populateServerArrayNodes.duration')).time()

        /**
         * Create a nodeset for query the result.
         */
        def nodeset = new NodeSetImpl();
        /**
         * List the ServerArrays
         */
        def serverArrays = api.getServerArrays().values()
        System.out.println("DEBUG: Iterating over ${serverArrays.size()} server arrays")
        logger.info("Retrieved ${serverArrays.size()} server arrays")
        serverArrays.each { serverArray ->
            def server_array_id = serverArray.getId()
            logger.info("Retrieving instances for array: " + serverArray.attributes['name'])
            println("DEBUG: Retrieving instances for array: " + serverArray.attributes['name'])
            /**
             * Get the Instances for this array
             */
            def instances = api.getServerArrayInstances(server_array_id)
            // Only include instances that are operational.
            def operationalInstances = instances.values().findAll { "operational".equalsIgnoreCase(it.attributes['state']) }
            logger.info("Retrieved ${operationalInstances.size()} operational instances.")
            System.out.println("DEBUG: Retrieved ${operationalInstances.size()} operational instances.")

            operationalInstances.each { instance ->
                /**
                 * Populate the Node entry with the instance data.
                 */
                System.out.println("DEBUG: Creating node for array, ${serverArray.attributes['name']}, instance: ${instance.attributes['name']}")
                logger.info("Creating node for array, ${serverArray.attributes['name']}, instance: ${instance.attributes['name']}")

                def NodeEntryImpl newNode = createNode(instance.attributes['name'])

                instance.populate(newNode)
                newNode.setNodename(instance.attributes['name'])  // TODO: Convention agreement.

                populateInstanceResources(api, instance, newNode)

                serverArray.populate(newNode)

                nodeset.putNode(newNode)
                System.out.println("DEBUG: Added ${serverArray.attributes['name']} server array instance: ${instance.attributes['name']}")
                logger.info("Added ${serverArray.attributes['name']} server array instance: ${instance.attributes['name']}")
            }
        }
        def duration = (System.currentTimeMillis() - starttime)
        System.println("DEBUG: populateServerArrayNodes() ended. (nodes=${nodeset.getNodes().size()}, duration=${duration})")
        logger.info("populateServerArrayNodes() ended. (nodes=${nodeset.getNodes().size()}, duration=${duration})")
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
    void populateInstanceResources(RightscaleAPI api, InstanceResource instance, NodeEntryImpl newNode) {
        System.out.println("DEBUG: Populating node for instance: ${instance.links['self']}.")

        def long starttime = System.currentTimeMillis()
        def timer = metrics.timer(MetricRegistry.name(RightscaleNodes, 'populateInstanceResources.duration')).time()

        def cloud_id = instance.links['cloud'].split("/").last()

        instance.links.each { rel, href ->
            switch (rel) {
                case "cloud":
                    def cloud = api.getClouds().get(instance.links['cloud'])
                    cloud.populate(newNode)
                    break
                case "datacenter":
                    def datacenter = api.getDatacenters(cloud_id).get(instance.links['datacenter'])
                    if (null != datacenter) {
                        datacenter.populate(newNode)
                    }
                    break
                case "deployment":
                    def deployment = api.getDeployments().get(instance.links['deployment'])
                    deployment.populate(newNode)
                    break
                case "image":
                    def image = api.getImages(cloud_id).get(instance.links['image'])
                    if (null != image) {
                        image.populate(newNode)
                    }
                    break
                case "inputs":
                    def inputs = api.getInputs(instance.links['inputs'])
                    inputs.values().each { input ->
                        if (input.attributes['name'].matches(configuration.getProperty(RightscaleNodesFactory.INPUT_PATT))) {
                            input.populate(newNode)
                            logger.info("Setting node attribute for input: ${input.attributes['name']}")
                            System.out.println("DEBUG: Setting node attribute for input: ${input.attributes['name']}")
                        } else {
                            logger.info("Ignored input ${input.attributes['name']}. Did not match: " + configuration.getProperty(RightscaleNodesFactory.INPUT_PATT))
                            System.out.println("DEBUG: Ignored input ${input.attributes['name']}. Did not match: " + configuration.getProperty(RightscaleNodesFactory.INPUT_PATT))
                        }
                    }
                    break;
                case "instance_type":
                    def instance_type = api.getInstanceTypes(cloud_id).get(instance.links['instance_type'])
                    if (null != instance_type) {
                        instance_type.populate(newNode)
                    }
                    break
                case "server_template":
                    def server_template = api.getServerTemplates().get(instance.links['server_template'])
                    if (null != server_template) {
                        server_template.populate(newNode)
                    }
                    break
                case "ssh_key":
                    def ssh_key = api.getSshKeys(cloud_id).get(instance.links['ssh_key'])
                    if (null != ssh_key) {
                        ssh_key.populate(newNode)
                    }
                    break
            }
        }
        System.out.println("DEBUG: Populating Tags for instance: " + instance.links['self'])
        logger.info("retrieving tags for instance: " + instance.links['self'])
        def tags = api.getTags(instance.links['self'])
        tags.values().each { TagsResource tag ->
            tag.attributes['tags'].split(",").each { name ->

                if (name.matches(configuration.getProperty(RightscaleNodesFactory.TAG_PATT))) {

                    /**
                     * Generate an attribute if the tag contains an equal sign.
                     */
                    if (Boolean.parseBoolean(configuration.getProperty(RightscaleNodesFactory.TAG_ATTR)) && tag.hasAttributeForm(name)) {
                        System.out.println("DEBUG: mapping tag to node attribute: ${name}.")
                        logger.info("mapping tag to node attribute: ${name}.")
                        tag.setAttribute(name, newNode)
                    } else {
                        RightscaleResource.setTag(name, newNode)
                        logger.info("setting tag: ${name}")
                        System.out.println("DEBUG: setting tag: ${name}")
                    }

                } else {
                    logger.info("Ignoring tag ${name}. Did not match ${configuration.getProperty(RightscaleNodesFactory.TAG_PATT)}")
                    System.out.println("DEBUG: Ignoring tag ${name}. Did not match ${configuration.getProperty(RightscaleNodesFactory.TAG_PATT)}")
                }
            }
        }
        def duration = (System.currentTimeMillis() - starttime)
        System.println("DEBUG: populateInstanceResources() ended. (duration=${duration})")
        logger.info("populateInstanceResources() ended. (duration=${duration})")
        timer.stop()
    }
}
