package com.simplifyops.rundeck.plugin.resources

import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.resources.ResourceModelSource
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException

import org.apache.log4j.Logger;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.LoggingFilter
import com.sun.jersey.api.representation.Form

import groovyx.gpars.GParsPool

public class RightscaleNodes implements ResourceModelSource {
    static Logger logger = Logger.getLogger(RightscaleNodes.class);

    private String email;
    private String password;
    private String account;

    private long refreshInterval;
    private String endpoint;

    private long lastRefresh = 0;

    private INodeSet nodeset;


    public RightscaleNodes(Properties configuration) {

        email = configuration.getProperty(RightscaleNodesFactory.EMAIL)
        password = configuration.getProperty(RightscaleNodesFactory.PASSWORD)
        account = configuration.getProperty(RightscaleNodesFactory.ACCOUNT)
        endpoint = configuration.getProperty(RightscaleNodesFactory.ENDPOINT)

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

            if (! needsRefresh()) {
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
     * Update the NodeSet and reset the last refresh time.
     * @param nodeset
     */
    void updateNodeSet(final INodeSet nodeset) {
        this.nodeset = nodeset;
        lastRefresh = System.currentTimeMillis();
    }

    /**
     * Query the RightScale API for servers and map them to Nodes.
     *
     * @return nodeset of Nodes
     */
    INodeSet query() {
        /**
         * Setup API defaults
         */
        Rest.defaultHeaders = ["X-API-VERSION": "1.5"]
        Rest.baseUrl = endpoint;

        /**
         * Login and create a session
         */
        authenticate();

        /**
         * Request the servers data as XML
         */
        def serversRequest = "/api/servers.xml" as Rest;
        serversRequest.addFilter(new LoggingFilter(System.out)); // debug output
        def ClientResponse serversResponse = serversRequest.get([:], [view: "instance_detail"]); // instance_detail contains extra info

        if ( serversResponse.status != 200 ) {
            throw new ResourceModelSourceException("RightScale servers request error. " + serversResponse)
        }
        /**
         * Create a node set for the result
         */
        INodeSet nodeset = new NodeSetImpl();

        /**
         * Traverse the dom to get each of the server nodes
         */
        def groovy.util.Node servers = serversResponse.XML
        System.out.println("DEBUG: number of servers in response to GET /api/servers: " + servers.server.size())
        servers.server.each { svr ->
            System.out.println("DEBUG: server: " + svr)
            /**
             * If it doesn't have a name ignore it.
             */
            if (null != svr.name.text()) {

                // ignore servers that aren't in a "running" state. They won't have a current_instance.
                def current_instance = svr.links.link.find { it.'@rel' == 'current_instance' }?.'@href'
                if (  null != current_instance ) {
                    System.out.println("DEBUG: current_instance: " + current_instance)

                    // Define a new Node
                    NodeEntryImpl newNode = new NodeEntryImpl(svr.name.text());
                    newNode.setDescription(svr.description.text())
                    newNode.setAttribute("rs:state", svr.state.text())
                    newNode.setAttribute("rs:created_at", svr.created_at.text())
                    /**
                     * Add the new node to the set
                     */
                    nodeset.putNode(newNode);

                    /**
                     * Make an additional RightScale API request to get Instance level data
                     */
                    def instanceRequest = current_instance as Rest
                    instanceRequest.addFilter(new LoggingFilter(System.out))  // debug output

                    def ClientResponse instanceResponse = instanceRequest.get([:], [view: "extended"])
                    if ( instanceResponse.status != 200 ) {
                        throw new ResourceModelSourceException("RightScale instance request error. " + serversResponse)
                    }
                    def groovy.util.Node instance = instanceResponse.XML
                    newNode.setAttribute("rs:resource_uid", instance.resource_uid.text())
                    newNode.setAttribute("rs:public_ip_address0", instance.public_ip_addresses.public_ip_address[0].text())
                    newNode.setAttribute("rs:private_ip_address0", instance.private_ip_addresses.private_ip_address[0].text())
                } else {
                    System.out.println("DEBUG: skipping server with a null current_instance ")

                }
            } else {
                System.out.println("DEBUG WARN: skipping server with a null name ")
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
     * Login and create a session
     */
    private void authenticate() {
        // add a filter to set cookies received from the server and to check if login has been triggered
        Rest.client.addFilter(new ClientFilter() {
            private ArrayList<Object> cookies;

            @Override
            public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
                if (cookies != null) {
                    request.getHeaders().put("Cookie", cookies);
                }
                ClientResponse response = getNext().handle(request);
                // copy cookies
                if (response.getCookies() != null) {
                    if (cookies == null) {
                        cookies = new ArrayList<Object>();
                    }
                    // A simple addAll just for illustration (should probably check for duplicates and expired cookies)
                    cookies.addAll(response.getCookies());
                }
                return response;
            }
        });

        WebResource sessionRequest = Rest.client.resource("${endpoint}/api/session");
        Form form = new Form();
        form.putSingle("email", email);
        form.putSingle("password", password);
        form.putSingle("account_href", "/api/accounts/${account}");
        def ClientResponse response = sessionRequest.header("X-API-VERSION", "1.5")
                .type("application/x-www-form-urlencoded").post(ClientResponse.class, form);
        /**
         * Check the response for http status (eg, 20x)
         */
        if ( response.status != 204 ) {
            throw new ResourceModelSourceException("RightScale login error. " + response)
        }
    }
}