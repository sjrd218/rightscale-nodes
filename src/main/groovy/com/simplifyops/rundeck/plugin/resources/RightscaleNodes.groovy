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



    @Override
    public synchronized INodeSet getNodes() throws ResourceModelSourceException {
        System.println("Inside getNodes()...")

        /**
         * Query Rightscale for their nodes
         */
        nodeset = query();

        /**
         * Return the nodeset
         */
        return nodeset;
    }

    INodeSet query() {
        /**
         * Login and create a session
         */
        connect();
        /**
         * Request the server data as XML
         */
        Rest.defaultHeaders = ["X-API-VERSION": "1.5"]
        Rest.baseUrl = endpoint;

        def serversXml = "/api/servers.xml" as Rest;
        serversXml.addFilter(new LoggingFilter(System.out))
        def ClientResponse response = serversXml.get([:], [view:"instance_detail"])

        /**
         * Create a new node set for the result
         */
        INodeSet nodeset = new NodeSetImpl();

        /**
         * Traverse the dom to get each of the server nodes
         */
        def groovy.util.Node servers = response.XML
        System.out.println("DEBUG: number of servers in response to GET /api/servers: " + servers.server.size())
        servers.server.each { svr ->
            System.out.println("DEBUG: server: " + svr)
            if (null != svr.name.text()) {
                def current_instance = svr.links.link.find { it.'@rel' == 'current_instance' }?.'@href'
                // a server with a current_instance is running.
                if (null != current_instance) {
                    System.out.println("DEBUG: current_instance: " + current_instance)

                    def instanceXml = current_instance as Rest
                    instanceXml.addFilter(new LoggingFilter(System.out))

                    def ClientResponse response2 = instanceXml.get([:], [view:"extended"])

                    def groovy.util.Node instance = response2.XML

                    System.out.println("Debug: adding server: " + svr.name.text())
                    NodeEntryImpl newNode = new NodeEntryImpl(svr.name.text());
                    newNode.setDescription(svr.description.text())
                    newNode.setAttribute("rs:state", svr.state.text())
                    newNode.setAttribute("rs:created_at", svr.created_at.text())

                    newNode.setAttribute("rs:resource_uid", instance.resource_uid.text())

                    newNode.setAttribute("rs:public_ip_address0", instance.public_ip_addresses.public_ip_address[0].text())
                    newNode.setAttribute("rs:private_ip_address0", instance.private_ip_addresses.private_ip_address[0].text())

                    nodeset.putNode(newNode);
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


    private void connect() {
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

        WebResource login = Rest.client.resource("${endpoint}/api/session");
        Form form = new Form();
        form.putSingle("email", email);
        form.putSingle("password", password);
        form.putSingle("account_href", "/api/accounts/${account}");
        def ClientResponse response = login.header("X-API-VERSION", "1.5").type("application/x-www-form-urlencoded").post(form);
        /**
         * Check the response for http status (eg, 20x)
         */
        //def statusCode = response.getClientResponseStatus().statusCode;
        //if (!( statusCode >= 200 && statusCode < 300)) {
        //    throw new ResourceModelSourceException("Rightscale login error. " + response)
        //}
    }
}