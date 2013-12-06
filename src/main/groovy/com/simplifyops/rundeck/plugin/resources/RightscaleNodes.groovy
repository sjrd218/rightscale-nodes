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

    private RightscaleQuery query;

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

        query = new RightscaleQuery(email, password, account, endpoint)

        println("DEBUG: New RightscaleNodes object created.")
    }

    /**
     * validate required params are set. Used by factory
     * @throws ConfigurationException
     */
    public void validate() throws ConfigurationException {
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
     * Query RightScale for their servers and return them as Nodes.
     */
    @Override
    public synchronized INodeSet getNodes() throws ResourceModelSourceException {
        System.println("DEBUG: Inside getNodes()...")

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

        /**
         * Return the nodeset
         */
        return nodeset;
    }

    /**
     * Returns true if the last refresh time was longer ago than the refresh interval
     */
    private boolean needsRefresh() {
        return refreshInterval < 0 || (System.currentTimeMillis() - lastRefresh > refreshInterval);
    }

    /**
     * Update the NodeSet and reset the last refresh time.
     * @param nodeset
     */
    void updateNodeSet(final INodeSet nodeset) {
        this.nodeset = nodeset;
        lastRefresh = System.currentTimeMillis();
    }

    private setNodeAttribute(NodeEntryImpl node, String key, String value) {
        node.setAttribute(prefix + key, value)
    }

    /**
     * Query the RightScale API for servers and map them to Nodes.
     *
     * @return nodeset of Nodes
     */
    INodeSet query() {

        INodeSet nodeset = new NodeSetImpl();

        nodeset.putNodes(queryServers(query))

        nodeset.putNodes(queryServerArrays(query))

        /**
         * Return the nodeset
         */
        return nodeset;
    }
    /**
     * Query all servers
     * @param query
     * @return
     */
    private INodeSet queryServers(RightscaleQuery query) {
        /**
         * List Servers
         */
        def servers = query.listServers()

        /**
         * Create a node set for the result
         */
        INodeSet nodeset = new NodeSetImpl();

        System.out.println("DEBUG: Iterating over ${servers.size()} servers")
        servers.each { href, svr ->
            System.println("DEBUG: processing server: " + svr.name)
            // Define a new Node
            NodeEntryImpl newNode = createNode(svr.name)
            newNode.setDescription(svr.description)
            setNodeAttribute(newNode, "state", svr.state)
            setNodeAttribute(newNode, "created_at", svr.created_at)
            /**
             * Use the server's deployment name as a tag and attribute.
             */
            def deployment = query.getDeployment(svr.deployment_href)
            setNodeAttribute(newNode, "deployment", deployment.name)
            if (!newNode.tags.contains("rs:${deployment.name}")) newNode.tags.add("rs:${deployment.name}")
            /**
             * Get the Tags for this Server.
             */
            def tags = query.getTags(svr.href)
            tags.each {
                if (!newNode.tags.contains(it)) newNode.tags.add("rs:${it}")
            }
            setNodeAttribute(newNode, "tags", tags.join(","))

            /**
             * Get this server's Instance as it contains attributes and links to more data.
             */
            def instance = query.getInstance(svr.current_instance_href)
            /**
             * Populate the node with the instance model data.
             */
            fillNode(query, newNode, instance)
            /**
             * Add the new node to the nodeset.
             */
            nodeset.putNode(newNode);
        }
        return nodeset
    }


    /**
     * Make nodes from ServerArray instances.
     * @param query
     * @return a new INodeSet of nodes for each instance in the query result.
     */
    private INodeSet queryServerArrays(RightscaleQuery query) {
        /**
         * Create a node set for the result
         */
        def nodeset = new NodeSetImpl();

        def instances = query.listServerArrayInstances()
        instances.each { href, model ->
            println("DEBUG: queryServerArrays: creating node for : " + model.name)

            NodeEntryImpl newNode = createNode(model.name)
            fillNode(query, newNode, model)
            if (model.containsKey("server_array")) {
                setNodeAttribute(newNode, "server_array", model.server_array)
                def tagname = "rs:array=${model.server_array}"
                if (!newNode.tags.contains(tagname)) newNode.tags.add(tagname)
            }

            nodeset.putNode(newNode)
        }

        return nodeset;
    }

    /**
     * Convenience method to create a new Node with defaults.
     * @param name Name of the node
     * @return a new Node
     */
    private NodeEntryImpl createNode(final String name) {
        NodeEntryImpl newNode = new NodeEntryImpl(name);
        newNode.setUsername(username) // - Config property.
        newNode.setOsFamily("unix");  // - Hard coded.
        return newNode
    }

    /**
     * Fill node with generic data from instance model.
     * @param query The rightscale query object
     * @param newNode The Node entry to fill
     * @param instance The map containing instance data.
     */
    private void fillNode(RightscaleQuery query, NodeEntryImpl newNode, Map instance) {
        setNodeAttribute(newNode, "resource_uid", instance.resource_uid)

        setNodeAttribute(newNode, "public_ip_address", instance.public_ip_address)
        setNodeAttribute(newNode, "private_ip_address", instance.private_ip_address)
        setNodeAttribute(newNode, "public_dns_name", instance.public_dns_name)
        setNodeAttribute(newNode, "private_dns_name", instance.private_dns_name)
        setNodeAttribute(newNode, "user_data", instance.user_data)

        newNode.setHostname(instance.public_ip_address) // TODO: convention needed here.

        /**
         * Get the ServerTemplate name
         */
        def server_template = query.getServerTemplate(instance.server_template_href)
        setNodeAttribute(newNode, "server_template", server_template.name)

        /**
         * Get the Cloud name and type
         */
        def cloud = query.getCloud(instance.cloud_href)
        setNodeAttribute(newNode, "cloud", cloud.name)
        setNodeAttribute(newNode, "cloud_type", cloud.cloud_type)
        if (!newNode.tags.contains("rs:${cloud.name}")) newNode.tags.add("rs:${cloud.name}")

        /**
         * Get the Deployment
         */
        def deployment = query.getDeployment(instance.deployment_href)
        setNodeAttribute(newNode, "deployment", deployment.name)
        if (!newNode.tags.contains("rs:${deployment.name}")) newNode.tags.add("rs:${deployment.name}")

        /**
         * Get the Datacenter name
         */
        if (instance?.datacenter_href) {
            def datacenter = query.getDatacenter(instance.datacenter_href)
            setNodeAttribute(newNode, "datacenter", datacenter.name)
            if (!newNode.tags.contains("rs:${datacenter.name}")) newNode.tags.add("rs:${datacenter.name}")
        }
        /**
         * Get the Subnet name and state
         */
        if (instance?.subnet_href) {
            def subnet = query.getSubnet(instance.subnet_href)
            setNodeAttribute(newNode, "subnet", subnet.name)
            setNodeAttribute(newNode, "subnet_visibility", subnet.state)
            setNodeAttribute(newNode, "subnet_state", subnet.visibility)
        }
        /**
         * Get the Image to get at cpu architecture
         */
        if (instance?.image_href) {
            def image = query.getImage(instance.image_href)
            setNodeAttribute(newNode, "image", image.name)
            setNodeAttribute(newNode, "cpu_architecture", image.cpu_architecture)
            setNodeAttribute(newNode, "virtualization_type", image.virtualization_type)
            newNode.setOsArch(image.cpu_architecture)  // TODO: convention here
        }
        /**
         * Get the InstanceType for machine level info.
         */
        if (instance?.instance_type_href) {
            def instance_type = query.getInstanceType(instance.instance_type_href)
            setNodeAttribute(newNode, "instance_type", instance_type.name)
            setNodeAttribute(newNode, "memory", instance_type.memory)
            setNodeAttribute(newNode, "cpu_speed", instance_type.cpu_speed)
            setNodeAttribute(newNode, "local_disks", instance_type.local_disks)
        }

        /**
         * Get the Tags for this Instance.
         */
        def tags = query.getTags(instance.href)
        tags.each {
            if (!newNode.tags.contains(it)) newNode.tags.add("rs:${it}")
        }
        setNodeAttribute(newNode, "tags", tags.join(","))

        /**
         * Get the ResourceInputs for this instance
         */

        def inputs = []
        query.getResourceInputs(cloud.href.split("/").last(), instance.href.split("/").last()).each {
            inputs.addAll(it)
        }
        setNodeAttribute(newNode, "inputs", inputs.join(","))

    }

}