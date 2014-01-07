package com.simplifyops.rundeck.plugin.resources

import com.codahale.metrics.MetricRegistry
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException
import groovyx.gpars.GParsPool
import org.apache.log4j.Logger

/**
 *  Load the cache from a query object. Subclasses must implement the #load method
 *  to define their own strategy for populating the cache using the query object data.
 */
public abstract class CacheLoader {
    abstract void load(RightscaleCache cache, RightscaleAPI query);
    public static String STRATEGY_V1 = "v1"
    public static String STRATEGY_V2 = "v2"

    public static CacheLoader create(String strategy) {
        switch (strategy) {
            case STRATEGY_V1:
                return new CacheLoader_v1()
                break
            case STRATEGY_V2:
                return new CacheLoader_v2()
                break
            default:
                throw new IllegalArgumentException("Unknown strategy: " + strategy)
        }
    }
}


class CacheLoader_v1 extends CacheLoader {
    boolean cachePrimed
    private MetricRegistry metrics = RightscaleNodesFactory.metrics
    static Logger logger = Logger.getLogger(CacheLoader.class);

    @Override
    void load(RightscaleCache cache, RightscaleAPI query) {
        def timer = metrics.timer(MetricRegistry.name(CacheLoader.class, 'load.duration')).time()

        def long starttime = System.currentTimeMillis()
        logger.info("loadCache() started.")
        System.out.println("DEBUG: loadCache() started.")

        if (!cachePrimed) {
            logger.info("loadCache() Priming cache")
            System.out.println("DEBUG: loadCache() Priming cache")
            def t = metrics.timer(MetricRegistry.name(CacheLoader.class, "priming.duration")).time()
            GParsPool.withPool {
                GParsPool.executeAsyncAndWait(
                        { cache.updateClouds(query.getClouds()) },
                        { cache.updateDeployments(query.getDeployments()) },
                        { cache.updateServerTemplates(query.getServerTemplates()) }
                )
            }
            def clouds = cache.getClouds().values()
            GParsPool.withPool {
                clouds.eachParallel { cloud ->
                    def cloud_id = cloud.getId()
                    System.out.println("DEBUG: Loading cache with resources for cloud: ${cloud.attributes['name']}")
                    logger.info("Loading cache with resources for cloud: ${cloud.attributes['name']}")

                    GParsPool.withPool {
                        GParsPool.executeAsyncAndWait(
                                { cache.updateDatacenters(query.getDatacenters(cloud_id)) },
                                { cache.updateImages(query.getImages(cloud_id)) },
                                { cache.updateInstanceTypes(query.getInstanceTypes(cloud_id)) },
                                { cache.updateSubnets(query.getSubnets(cloud_id)) },
                                { cache.updateSshKeys(query.getSshKeys(cloud_id)) }
                        )
                    }
                }
            }
            cachePrimed = true;
            logger.info("loadCache() cache prime complete")
            System.out.println("DEBUG: loadCache() cache prime complete")
            t.stop()

        }

        /**
         * Get the Instances.
         */
        def clouds = cache.getClouds().values()
        GParsPool.withPool {
            clouds.eachParallel { cloud ->
                def cloud_id = cloud.getId()

                cache.updateInstances(query.getInstances(cloud_id))

            }
        }
        // Filter on instances that are in the 'operational' state.
        def operationalInstances = cache.getInstances().values().findAll {
            "operational".equalsIgnoreCase(it.attributes['state'])
        }
        System.out.println("DEBUG: Cache contains ${operationalInstances.size()} operational instances.")
        logger.info("Cache contains ${operationalInstances.size()} operational instances.")

        /**
         * Post process the Instances to gather other linked resources. Only instances in the operational state are proessed..
         *  - Images: It's assumed that instances share a small set of base images. Get only the ones in use.
         *  - Tags: Tags aren't referenced as a link and must be searched for.
         *  - Inputs: Resource Inputs must also be searched for.
         */

        Map<String, RightscaleResource> tags = [:]
        def instanceTimer = metrics.timer(MetricRegistry.name(CacheLoader.class, 'instance.duration'))

        operationalInstances.each { instance ->
            def t = instanceTimer.time()
            GParsPool.withPool {
                GParsPool.executeAsyncAndWait(

                        {
                            // Get the Inputs and update the cache with them.
                            System.out.println("DEBUG: Query inputs, ${instance.links['inputs']} for instance: ${instance.links['self']}.")
                            if (!cache.hasResource('inputs', instance.links['inputs'])) {
                                cache.updateInputs(query.getInputs(instance.links['inputs']))
                            }
                        },
                        {
                            // Get the Tags.
                            System.out.println("DEBUG: Query tags for instance: ${instance.links['self']}.")
                            def linkedTags = query.getTags(instance.links['self']).values()
                            System.out.println("DEBUG: Query result found ${linkedTags.size()} tags for instance ${instance.links['self']}.")
                            linkedTags.each { tag ->
                                System.out.println("DEBUG: Caching tags: \"" + tag.attributes['tags'] + "\" for instance: " + instance.attributes['name'])
                                tags.put(instance.links['self'], tag)
                            }
                        }
                )
            }
            t.stop()
        }

        cache.updateTags(tags)

        /**
         * Get the Servers
         */
        cache.updateServers(query.getServers())
        /**
         *  Get the ServerArrays
         */
        cache.updateServerArrays(query.getServerArrays())
        def serverArrays = cache.getServerArrays().values()

        System.out.println("DEBUG: Querying instances for ${serverArrays.size()} server arrays.")
        logger.info("Querying instances for ${serverArrays.size()} server arrays.")
        def severArrayTimer = metrics.timer(MetricRegistry.name(CacheLoader.class, 'server_array.duration'))

        GParsPool.withPool {
            serverArrays.eachParallel {
                System.out.println("DEBUG: Querying instances for server array: ${it.attributes['name']}.")
                logger.info("Querying instances for server array: ${it.attributes['name']}.")
                def t = severArrayTimer.time()
                def server_array_id = it.getId()
                cache.updateServerArrayInstances(query.getServerArrayInstances(server_array_id))
                t.stop()
            }
        }

        /**
         * Done loading the cache.
         */
        def endtime = System.currentTimeMillis()
        def duration = (endtime - starttime)
        System.out.println("DEBUG: loadCache() completed. (resources=${cache.size()}, duration: ${duration})")
        logger.info("loadCache() completed. (resources=${cache.size()}, duration: ${duration})")
        timer.stop()
    }
}


class CacheLoader_v2 extends CacheLoader {

    boolean cachedPrimary
    boolean cachedSecondary
    private MetricRegistry metrics = RightscaleNodesFactory.metrics
    static Logger logger = Logger.getLogger(CacheLoader.class);


    @Override
    void load(RightscaleCache cache, RightscaleAPI query) {
        def timer = metrics.timer(MetricRegistry.name(CacheLoader.class, 'load.duration')).time()

        def long starttime = System.currentTimeMillis()
        logger.info("Cache loader started.")
        System.out.println("DEBUG: Cache loader started. (strategy: ${STRATEGY_V2})")


        loadPrimary(cache, query)


        if (cachedSecondary) {
            logger.info("Secondary resources already cached.")
            println("DEBUG: Secondary resources already cached.")
        } else {
            try {
                loadSecondary(cache, query)
            } catch (RightscaleAPIRequest.RequestException e) {
                logger.error("Cache load caught API request error while loading secondary data. Continuing.")
                println("DEBUG: Cache load caught API request error while loading secondary data. Continuing.")
                e.printStackTrace()
                metrics.counter(MetricRegistry.name(CacheLoader.class, "load.secondary.error")).inc();
            } catch (Exception ex) {
                throw new CacheLoadException(ex)
            }
        }

        /**
         * Done loading the cache.
         */
        def endtime = System.currentTimeMillis()
        def duration = (endtime - starttime)
        System.out.println("DEBUG: Cache loader completed. (resources=${cache.size()}, duration: ${duration}, strategy: ${STRATEGY_V2})")
        logger.info("Cache loader completed. (resources=${cache.size()}, duration: ${duration}, strategy: ${STRATEGY_V2})")
        timer.stop()
    }
    
    /**
     * Get the essentials. Clouds, Instances (their Tags and Inputs), Servers and ServerArrays.
     * @param cache
     * @param query
     */
    private void loadPrimary(RightscaleCache cache, RightscaleAPI query) {
        def loadTimer = metrics.timer(MetricRegistry.name(CacheLoader.class, "primary.duration")).time()

        /**
         * Clear cache of the primary resources
         */
        cache.clearInstances()
        cache.clearServers();
        cache.clearServerArrays();
        cache.clearServerArrayInstances()
        cache.clearInputs()
        cache.clearTags()

        /**
         * Get the Clouds.
         */
        cache.updateClouds(query.getClouds())

        /**
         * Get the Instances in parallel.
         */
        def clouds = cache.getClouds().values()
        GParsPool.withPool {
            clouds.eachParallel { cloud ->
                cache.updateInstances(query.getInstances(cloud.getId()))
            }
        }
        // Filter on instances that are in the 'operational' state.
        def operationalInstances = cache.getInstances().values().findAll {
            "operational".equalsIgnoreCase(it.attributes['state'])
        }
        System.out.println("DEBUG: Cache contains ${operationalInstances.size()} operational instances.")
        logger.info("Cache contains ${operationalInstances.size()} operational instances.")

        /**
         * Post process the Instances to gather their Tags and Inputs
         */
        Map<String, RightscaleResource> tags = [:]
        def instanceTimer = metrics.timer(MetricRegistry.name(CacheLoader.class, 'instance.duration'))

        operationalInstances.each { instance ->
            def t = instanceTimer.time()

            GParsPool.withPool {
                GParsPool.executeAsyncAndWait(

                        {
                            // Get the Inputs and update the cache with them.
                            System.out.println("DEBUG: Cache loader quering inputs for instance: ${instance.links['self']}.")
                            def inputs = query.getInputs(instance.links['inputs'])
                            cache.updateInputs(inputs)
                            System.out.println("DEBUG: Cache contains ${inputs.size()} inputs for instance: ${instance.links['self']}.")

                        },
                        {
                            // Get the Tags.
                            System.out.println("DEBUG: Cache load quering tags for instance: ${instance.links['self']}.")
                            def linkedTags = query.getTags(instance.links['self']).values()
                            System.out.println("DEBUG: Cache contains ${linkedTags.size()} tags for instance ${instance.links['self']}.")
                            linkedTags.each { tag ->
                                tags.put(instance.links['self'], tag)
                            }
                        }
                )
            }
            t.stop()
        }

        cache.updateTags(tags)

        /**
         * Get the Servers
         */
        cache.updateServers(query.getServers())
        /**
         *  Get the ServerArrays
         */

        cache.updateServerArrays(query.getServerArrays())
        def serverArrays = cache.getServerArrays().values()

        System.out.println("DEBUG: Cache loader querying instances for ${serverArrays.size()} server arrays.")
        logger.info("Cache loader querying instances for ${serverArrays.size()} server arrays.")
        def severArrayTimer = metrics.timer(MetricRegistry.name(CacheLoader.class, 'server_array.instances.duration'))

        GParsPool.withPool {
            serverArrays.eachParallel {
                System.out.println("DEBUG: Cache loader querying current_instances for server array: ${it.attributes['name']}.")
                logger.info("Cache loader querying current_instances for server array: ${it.attributes['name']}.")
                def t = severArrayTimer.time()
                def String server_array_id = it.getId()
                cache.updateServerArrayInstances(query.getServerArrayInstances(server_array_id))
                t.stop()
            }
        }
        cachedPrimary = true
        loadTimer.stop()
    }

    private void loadSecondary(RightscaleCache cache, RightscaleAPI query) {
        logger.info("Cache loading secondary resources into cache")
        System.out.println("DEBUG: Cache loading secondary resources into cache")
        def t = metrics.timer(MetricRegistry.name(CacheLoader.class, "secondary.duration")).time()
        GParsPool.withPool {
            GParsPool.executeAsyncAndWait(
                    { cache.updateDeployments(query.getDeployments()) },
                    { cache.updateServerTemplates(query.getServerTemplates()) }
            )
        }
        def clouds = cache.getClouds().values()
        GParsPool.withPool {
            clouds.eachParallel { cloud ->
                def cloud_id = cloud.getId()
                System.out.println("DEBUG: Cache loading resources for cloud: ${cloud.attributes['name']}")
                logger.info("Cache loading resources for cloud: ${cloud.attributes['name']}")

                GParsPool.withPool {
                    GParsPool.executeAsyncAndWait(
                            { cache.updateDatacenters(query.getDatacenters(cloud_id)) },
                            { cache.updateImages(query.getImages(cloud_id)) },
                            { cache.updateInstanceTypes(query.getInstanceTypes(cloud_id)) },
                            { cache.updateSubnets(query.getSubnets(cloud_id)) },
                            { cache.updateSshKeys(query.getSshKeys(cloud_id)) }
                    )
                }
            }
        }

        cachedSecondary = true;
        logger.info("Cache load secondary resources cached")
        System.out.println("DEBUG: Cache load econdary resources cached")
        t.stop()
    }

    class CacheLoadException extends ResourceModelSourceException {

        public CacheLoadException() {
            super();
        }

        public CacheLoadException(String msg) {
            super(msg);
        }

        public CacheLoadException(Exception cause) {
            super(cause);
        }

        public CacheLoadException(String msg, Exception cause) {
            super(msg, cause);
        }

    }

}

