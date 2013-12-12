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
    // Prefix each attribute name with this string.
    private String prefix = "rs_"

    /**
     * Time nodes were last updated.
     */
    private long lastRefresh = 0;
    /**
     * The nodeset filled by the query result.
     */
    private INodeSet nodeset;

    private RightscaleAPI query;

    /**
     * Default constructor.
     * @param configuration Properties containing plugin configuration values.
     */
    public RightscaleNodes(Properties configuration) {

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

        query = new RightscaleAPIRequest(email, password, account, endpoint)

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

    /**
     * Query RightScale for their instances and return them as Nodes.
     */
    @Override
    public synchronized INodeSet getNodes() throws ResourceModelSourceException {
        System.println("DEBUG: Inside getNodes()...")
        def long starttime = System.currentTimeMillis()
        /**
         * Haven't got any nodes yet so get them synchronously.
         */
        if (null == nodeset) {
            System.println("DEBUG: Getting nodes synchronously first time.")
            updateNodeSet(query());

        } else {

            if (!needsRefresh()) {
                System.println("DEBUG: Nodes don't need a refresh.")
                return nodeset;
            }

            /**
             * Query asynchronously.
             */
            System.println("DEBUG: Asynchronously getting nodes.")
            Closure queryRequest = { updateNodeSet(query()) }
            GParsPool.withPool() {
                queryRequest.callAsync().get()
            }
        }

        System.println("DEBUG: query time: " + (System.currentTimeMillis() - starttime))
        /**
         * Return the nodeset
         */
        return nodeset;
    }

    /**
     * Returns true if the last refresh time was longer ago than the refresh interval.
     */
    private boolean needsRefresh() {
        return refreshInterval < 0 || (System.currentTimeMillis() - lastRefresh > refreshInterval);
    }

    /**
     * Update the NodeSet and reset the last refresh time.
     * @param nodeset
     */
    private void updateNodeSet(final INodeSet nodeset) {
        this.nodeset = nodeset;
        lastRefresh = System.currentTimeMillis();
    }

    /**
     * Query the RightScale API for instances and map them to Nodes.
     *
     * @return nodeset of Nodes
     */
    INodeSet query() {

        INodeSet nodeset = new NodeSetImpl();

        nodeset.putNodes(queryServers(query))

        //nodeset.putNodes(queryServerArrays(query))

        /**
         * Return the nodeset
         */
        return nodeset;
    }
    /**
     * Query all servers and get their Instances as Nodes
     * @param query the RightscaleQuery
     * @return a node set of Nodes
     */
    INodeSet queryServers(RightscaleAPI query) {
        /**
         * Create a node set from the result.
         */
        def nodeset = new NodeSetImpl();
        /**
         * List the Servers
         */
        def servers = query.getServers().values()
        System.out.println("DEBUG: Iterating over ${servers.size()} servers")

        servers.each { server ->
            // Only process servers with a current instance.
            if (server.links.containsKey('current_instance')) {
                def NodeEntryImpl newNode = new NodeEntryImpl()
                server.populate(newNode)
                server.links.each { rel, href ->
                    if (!rel.equals('self') && query.getResources(rel).containsKey(href)) {
                        def RightscaleResource linkedResource = query.getResources(rel).get(href)
                        linkedResource.populate(newNode)
                    }
                }
                def cloud_href = server.links['cloud']
                if (null == cloud_href) {
                    throw new RuntimeException("cloud link not found for server: " + server.attributes['name'])
                }
                def cloud_id = cloud_href.split("/").last()
                def InstanceResource instance = query.getInstances(cloud_id).get(server.links['current_instance'])
                instance.populate(newNode)
                instance.links.each { rel, href ->
                    if (!rel.equals('self') && query.getResources(rel).containsKey(href)) {
                        def RightscaleResource linkedResource = query.getResources(rel).get(href)
                        linkedResource.populate(newNode)
                    }
                }
                // Add the node to the result.
                nodeset.putNode(newNode)
            }
        }
        return nodeset
    }

    /**
     * Make nodes from ServerArray instances.
     * @param query the RightscaleQuery
     * @return a new INodeSet of nodes for each instance in the query result.
     */
    INodeSet queryServerArrays(RightscaleAPI query) {
        /**
         * Create a node set from the result.
         */
        def nodeset = new NodeSetImpl();
        /**
         * List the ServerArrays
         */
        def serverArrays = query.getServerArrays().values()
        System.out.println("DEBUG: Iterating over ${serverArrays.size()} server arrays")
        serverArrays.each { serverArray ->
            //serverArray.attributes['instances_count']
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
        newNode.setOsFamily("unix");  // - Hard coded.
        return newNode
    }
}