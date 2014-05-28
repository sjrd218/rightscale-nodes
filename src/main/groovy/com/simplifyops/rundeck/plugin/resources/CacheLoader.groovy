package com.simplifyops.rundeck.plugin.resources

import com.codahale.metrics.MetricRegistry
import groovyx.gpars.GParsPool
import org.apache.log4j.Logger

/**
 *  Load the cache from a query object. Subclasses must implement the #load method
 *  to define their own strategy for populating the cache using the query object data.
 */
public abstract class CacheLoader {
    abstract void load(RightscaleCache cache, RightscaleAPI query);
    public static String STRATEGY_MINIMUM = "minimum"
    public static String STRATEGY_FULL = "full"

    public static CacheLoader create(String strategy) {
        switch (strategy) {
            case STRATEGY_MINIMUM:
                return new CacheLoader_MINIMUM()
                break
            case STRATEGY_FULL:
                return new CacheLoader_FULL()
                break
            default:
                throw new IllegalArgumentException("Unknown strategy: " + strategy)
        }
    }
}

class CacheLoader_MINIMUM extends CacheLoader {

    boolean cachedPrimary
    private MetricRegistry metrics = RightscaleNodesFactory.metrics
    static Logger logger = Logger.getLogger(CacheLoader.class);


    @Override
    void load(RightscaleCache cache, RightscaleAPI query) {
        def timer = metrics.timer(MetricRegistry.name(CacheLoader.class, 'load.duration')).time()

        def long starttime = System.currentTimeMillis()
        logger.info("Cache loader started.")
        System.out.println("DEBUG: Cache loader started. (strategy: ${STRATEGY_MINIMUM})")


        loadResources(cache, query)

        /**
         * Done loading the cache.
         */
        def endtime = System.currentTimeMillis()
        def duration = (endtime - starttime)
        System.out.println("DEBUG: Cache loader completed. (resources=${cache.size()}, duration: ${duration}, strategy: ${STRATEGY_MINIMUM})")
        logger.info("Cache loader completed. (resources=${cache.size()}, duration: ${duration}, strategy: ${STRATEGY_MINIMUM})")
        timer.stop()
    }

    /**
     * Get the essentials. Clouds, Instances (their Tags and Inputs), Servers and ServerArrays.
     * @param cache
     * @param query
     */
    void loadResources(RightscaleCache cache, RightscaleAPI query) {
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
}

class CacheLoader_FULL extends CacheLoader_MINIMUM {

    boolean cachedSecondary
    private MetricRegistry metrics = RightscaleNodesFactory.metrics
    static Logger logger = Logger.getLogger(CacheLoader.class);


    @Override
    void loadResources(RightscaleCache cache, RightscaleAPI query) {

        super.loadResources(cache, query)

        if (!cachedSecondary) {
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
                    System.out.println("DEBUG: Cache loading secondary resources for cloud: ${cloud.attributes['name']}")
                    logger.info("Cache loading secondary resources for cloud: ${cloud.attributes['name']}")

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
            System.out.println("DEBUG: Cache load secondary resources cached")
            t.stop()
        }
    }
}
