package com.simplifyops.rundeck.plugin.resources

import com.codahale.metrics.MetricRegistry
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException
import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.ClientHandlerException
import com.sun.jersey.api.client.ClientRequest
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.sun.jersey.api.client.config.ClientConfig
import com.sun.jersey.api.client.config.DefaultClientConfig
import com.sun.jersey.api.client.filter.ClientFilter
import com.sun.jersey.api.client.filter.LoggingFilter
import com.sun.jersey.api.representation.Form
import org.apache.log4j.Logger

class RightscaleAPIRequest implements RightscaleAPI {
    static Logger logger = Logger.getLogger(RightscaleAPIRequest.class);

    def String email
    def String password
    def String account
    def String endpoint
    def boolean debug

    def ApiClient restClient;
    def boolean authenticated = false;

    def int timeout = 0; // default timeout interval set to infinity.

    private MetricRegistry metrics = RightscaleNodesFactory.metrics;

    /**
     * Default constructor.
     */
    RightscaleAPIRequest(String email, String password, String account, String endpoint) {
        this.email = email
        this.password = password
        this.account = account
        this.endpoint = endpoint

        restClient = new ApiClient(endpoint)
        logger.info("RightscaleAPIRequest instantiated.")
    }

    RightscaleAPIRequest(Properties p) {
        this(p.getProperty(RightscaleNodesFactory.EMAIL),
                p.getProperty(RightscaleNodesFactory.PASSWORD),
                p.getProperty(RightscaleNodesFactory.ACCOUNT),
                p.getProperty(RightscaleNodesFactory.ENDPOINT))
        if (p.containsKey(RightscaleNodesFactory.HTTP_LOG)) {
            debug = Boolean.parseBoolean(p.getProperty(RightscaleNodesFactory.HTTP_LOG))
        }
        if (p.containsKey(RightscaleNodesFactory.HTTP_TIMEOUT)) {
            timeout = Integer.parseInt(p.getProperty(RightscaleNodesFactory.HTTP_TIMEOUT))
        }
    }

    public void initialize() {
        restClient.authenticate()
    }

    /**
     * Query API service for the Servers
     * @return Map mof models keyed by server href.
     */
    @Override
    public Map<String, RightscaleResource> getServers() {
        def Node xml = restClient.get("/api/servers", [view: "instance_detail"])
        def servers = ServerResource.burst(xml, 'server', ServerResource.&create)
        return servers
    }

    /**
     * Query API service for a list of instances for the cloud.
     * @param cloud_id
     * @return Map of models keyed by cloud href.
     */
    @Override
    public Map<String, RightscaleResource> getInstances(String cloud_id) {
        def Node xml = restClient.get("/api/clouds/${cloud_id}/instances", [view: "extended"])
        return InstanceResource.burst(xml, 'instance', InstanceResource.&create)
    }

    /**
     * Query Rightscale API service for all the deployments.
     * @return Map of Deployments keyed by their href
     */
    @Override
    public Map<String, RightscaleResource> getDeployments() {
        try {
            def Node xml = restClient.get("/api/deployments", [:])
            return DeploymentResource.burst(xml, 'deployment', DeploymentResource.&create)
        } catch (UnsupportedResourceType e) {
            logger.info("Return an empty list for unsupported resource type: deployments")
            return [:]
        }
    }

    /**
     * Query the Rightscale API for the Cloud resources.
     * @return Map of Clouds
     */
    @Override
    public Map<String, RightscaleResource> getClouds() {
        def Node xml = restClient.get("/api/clouds", [:])
        return CloudResource.burst(xml, 'cloud', CloudResource.&create)
    }

    /**
     * Query the Rightscale API for all ServerTemplate resources.
     * @return
     */
    @Override
    public Map<String, RightscaleResource> getServerTemplates() {
        def Node xml = restClient.get('/api/server_templates', [:])
        return ServerTemplateResource.burst(xml, 'server_template', ServerTemplateResource.&create)
    }

    /**
     * Query the Rightscale API for all Datacenter resources for the specified cloud.
     * @param cloud_id
     * @return
     */
    @Override
    public Map<String, RightscaleResource> getDatacenters(String cloud_id) {
        try {
            def Node xml = restClient.get("/api/clouds/${cloud_id}/datacenters", [:])
            return DatacenterResource.burst(xml, 'datacenter', DatacenterResource.&create)
        } catch (UnsupportedResourceType e) {
            logger.info("Return an empty list for unsupported resource type: datacenters")
            return [:]
        }
    }

    /**
     * Query the Rightscale API for a single Subnet resource.
     * @return Map of Subnets
     */
    @Override
    public Map<String, RightscaleResource> getSubnets(final String cloud_id) {
        try {
            def Node xml = restClient.get("/api/clouds/${cloud_id}/subnets", [:])
            return SubnetResource.burst(xml, 'subnet', SubnetResource.&create)
        } catch (UnsupportedResourceType e) {
            logger.info("Returning an empty list for unsupported resource type: subnets")
            return [:]
        }
    }

    /**
     * Query the Rightscale API for a single SshKey resource.
     * @return Map of SshKeys
     */
    @Override
    public Map<String, RightscaleResource> getSshKeys(final String cloud_id) {

        try {
            def Node xml = restClient.get("/api/clouds/${cloud_id}/ssh_keys", [:])
            return SshKeyResource.burst(xml, 'ssh_key', SshKeyResource.&create)
        } catch (UnsupportedResourceType e) {
            logger.info("Return an empty list for unsupported resource type: ssh_keys")
            return [:]
        }
    }

    /**
     * Query the Rightscale API for Image resources.
     * @return Map of Images
     */
    @Override
    public Map<String, RightscaleResource> getImages(final String cloud_id) {
        def Node xml = restClient.get("/api/clouds/${cloud_id}/images", [:])
        return ImageResource.burst(xml, 'image', ImageResource.&create)
    }
    /**
     * Query the Rightscale API for a single Image resource.
     * @return Map of Images
     */
    @Override
    public RightscaleResource getImage(final String href) {
        def Node xml = restClient.get(href, [:])
        return ImageResource.create(xml)
    }

    /**
     * Query the Rightscale API for all ResourceInput resources for the specified instance.
     * @param cloud_id
     * @param instance_id
     * @return List of maps containing name/value pairs.
     */

    @Override
    public Map<String, RightscaleResource> getInputs(final String href) {
        try {
            def Node xml = restClient.get(href, [:])
            return InputResource.burst(xml, 'input', InputResource.&create)
        } catch (UnsupportedResourceType e) {
            logger.info("Return an empty list for unsupported resource type: inputs")
            return [:]
        }
    }

    /**
     * Query the Rightscale API for all InstanceType resources for the specified cloud.
     * @param cloud_id
     * @return
     */
    @Override
    public Map<String, RightscaleResource> getInstanceTypes(String cloud_id) {
        try {
            def Node xml = restClient.get("/api/clouds/${cloud_id}/instance_types", [:])
            return InstanceTypeResource.burst(xml, 'instance_type', InstanceTypeResource.&create)
        } catch (UnsupportedResourceType e) {
            logger.info("Return an empty list for unsupported resource type: instance_type")
            return [:]
        }
    }

    /**
     * Query the Rightscale API for all ServerArray resources.
     * @return
     */
    @Override
    public Map<String, RightscaleResource> getServerArrays() {
        def Node xml = restClient.get("/api/server_arrays", [view: "instance_detail"])
        return ServerArrayResource.burst(xml, 'server_array', ServerArrayResource.&create)
    }

    @Override
    public Map<String, RightscaleResource> getServerArrayInstances(String server_array_id) {
        def Node xml = restClient.get("/api/server_arrays/${server_array_id}/current_instances",[:])
        return InstanceResource.burst(xml, 'instance', InstanceResource.&create)
    }

    /**
     * Query the Rightscale API for the tags assigned to the specified resource.
     * @return Map of Tags
     */
    @Override
    public Map<String, RightscaleResource> getTags(final String href) {
        def xml = restClient.post('/api/tags/by_resource',[:],["resource_hrefs[]": href])
        return TagsResource.burst(xml, 'resource_tag', TagsResource.&create)
    }




    /**
     * Helper class for making Rightscale API HTTP requests.
     */
    class ApiClient {

        private ArrayList<Object> cookies;
        private String baseUrl
        private long lastAuthentication=0L;

        private def errorCount = metrics.counter(MetricRegistry.name(RightscaleAPIRequest.class, "request.errors"));
        private def successCount = metrics.counter(MetricRegistry.name(RightscaleAPIRequest.class, "request.success"));
        private def authenticationCount = metrics.counter(MetricRegistry.name(RightscaleAPIRequest.class, "authentication"));
        private def failureCount = metrics.counter(MetricRegistry.name(RightscaleAPIRequest.class, "request.fail"));
        private def timer = metrics.timer(MetricRegistry.name(RightscaleAPIRequest, 'request.duration'))
        private ClientFilter clientAuthFilter
        ClientConfig cc

        ApiClient(baseUrl) {
            // API defaults
            this.baseUrl = baseUrl

            cc = new DefaultClientConfig();
            cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, timeout);
            cc.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, timeout);
            clientAuthFilter=makeSendCookieFilter()
        }

        /**
         * Returns a filter that will send all of the stored cookies in each request.
         * @param client
         */
        private ClientFilter makeSendCookieFilter() {
            return new ClientFilter() {
                @Override
                public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
                    if (cookies != null) {
                        request.getHeaders().put("Cookie", cookies);
                    }
                    return getNext().handle(request);
                }
            };
        }

        /**
         * Login and create a session.
         */
        synchronized void authenticate() {
            logger.info("Authenticating ${email}...")
            //reset cookies
            cookies = new ArrayList<Object>();
            //use a local Jersey client
            ClientConfig cc = new DefaultClientConfig();
            cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, timeout);
            cc.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, timeout);

            def client = Client.create(cc)

            //add a filter to store and send all cookies received from the server
            client.addFilter(new ClientFilter() {
                @Override
                public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
                    if (cookies != null) {
                        request.getHeaders().put("Cookie", cookies);
                    }
                    ClientResponse response = getNext().handle(request);
                    // copy cookies
                    if (response.getCookies() != null) {
                        cookies.addAll(response.getCookies());
                    }
                    return response;
                }
            });

            WebResource sessionRequest = client.resource("${endpoint}/api/session");
            Form form = new Form();
            form.putSingle("email", email);
            form.putSingle("password", password);
            form.putSingle("account_href", "/api/accounts/${account}");
            def ClientResponse response = sessionRequest.header("X-API-VERSION", "1.5")
                    .type("application/x-www-form-urlencoded").post(ClientResponse.class, form);
            /**
             * Check the response for http status (eg, 20x)
             */
            if (response.status != 204) {
                cookies == null
                logger.warn("RightScale login error. ")
                throw new RequestException("RightScale login error. " + response)
            }
            authenticated=true
            authenticationCount.inc()
            lastAuthentication=System.currentTimeMillis()
            logger.info("Successfully authenticated: ${email}")
        }

        /**
         * Calls authenticate only if last authentication was not within a certain amount of time
         * @param delay millisecond time delay between reauthentication
         * @return true if currently authenticated
         */
        synchronized boolean reauthenticate(long delay){
            if(System.currentTimeMillis() > (lastAuthentication + delay)){
                authenticated=false
                authenticate()
            }
            return authenticated
        }

        /**
         * Handle the request by calling a closure that returns a response, if
         * the response status id 403, attempt to reauthenticate and if successful retry the original request
         * @param makeRequest closure which makes the request and returns the response
         * @return the response.XML
         */
        Node handleRequest(href,Closure makeRequest){
            def ClientResponse response = makeRequest()

            if (response.status == 403) {
                //authenticate if not re-authenticated in the last 30 seconds
                if(reauthenticate(30*1000)){
                    //retry request only if we are now authenticated again
                    response=makeRequest()
                    System.out.println("DEBUG: Reauthenticating to service.")
                    logger.info("Reauthenticating to service.")

                }
            }
            if (response.status == 422) {
                // unsupported resource type
                System.out.println("DEBUG: Unsupported resource type: ${href}")
                logger.info("Unsupported resource type: ${href}")
                throw new UnsupportedResourceType("href: ${href}")
            }
            if (response.status != 200) {
                failureCount.inc()
                throw new RequestException("RightScale request error: ${href}: " + response)
            }

            return response.XML
        }

        /**
         * Create the Rest object for the given href
         * @param href
         * @return
         */
        private Rest buildRest(String href) {
            def request = new Rest(baseUrl, href, cc);
            request.headers = ["X-API-VERSION": "1.5"]
            if (debug) {
                request.addFilter(new LoggingFilter(System.out)); // debug output
            }
            request.addFilter(clientAuthFilter); // include cookie headers
            request
        }
        /**
         * Get the resource
         * @param href
         * @param params
         * @return Node containing the resource as XML data.
         */
        Node get(String href, Map params) {
            if (null == href) throw new IllegalArgumentException("href cannot be null")
            if (!href.endsWith('.xml')) {
                href= href+'.xml'
            }
            def long starttime = System.currentTimeMillis()
            System.out.println("DEBUG: Requesting resource href: ${href}.")
            logger.info("Requesting resource href: ${href}.")

            Rest request
            try {
                request = buildRest(href)
            } catch (Throwable t) {
                // TODO: Understand why this can be a Jersey SPI error. Thought it was caused by HTTP timeout.
                System.out.println("DEBUG: Caught buildRest exception for href ${href}. Throwable type: "+t.getClass().getName())
                errorCount.inc()
                throw new RequestException("Error while building a client for href ${href}: " + t.message)
            }
            def response=timer.time{
                handleRequest(href) {
                    request.get([:], params)
                }
            }

            def endtime = System.currentTimeMillis()
            def duration = (endtime - starttime)
            System.out.println("DEBUG: Request succeeded: href ${href}. (duration=${duration})")
            logger.info("Request succeeded: href ${href}. (duration=${duration})")

            successCount.inc()
            return response
        }

        Node post(String href, Map params, Map data) {
            def long starttime = System.currentTimeMillis()
            System.out.println("DEBUG: Requesting resource href: ${href}.")
            logger.info("Requesting resource href: ${href}.")

            def request = buildRest(href)

            def response=timer.time{
                handleRequest(href){
                    request.post({}, params, data)
                }
            }

            def endtime = System.currentTimeMillis()
            def duration = (endtime - starttime)
            System.out.println("DEBUG: Request succeeded: href ${href}. (duration=${duration})")
            logger.info("Request succeeded: href ${href}. (duration=${duration})")
            return response
        }
    }

    class RequestException extends ResourceModelSourceException {

        public RequestException() {
            super();
        }

        public RequestException(String msg) {
            super(msg);
        }

        public RequestException(Exception cause) {
            super(cause);
        }

        public RequestException(String msg, Exception cause) {
            super(msg, cause);
        }

    }

    class UnsupportedResourceType extends RequestException {
        public UnsupportedResourceType() {
            super();
        }

        public UnsupportedResourceType(String msg) {
            super(msg);
        }

        public UnsupportedResourceType(Exception cause) {
            super(cause);
        }

        public UnsupportedResourceType(String msg, Exception cause) {
            super(msg, cause);
        }
    }
}

