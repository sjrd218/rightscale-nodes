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

    def RestClient restClient;
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

        restClient = new RestClient(endpoint)
        logger.debug("RightscaleAPIRequest instantiated.")
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
            logger.debug("Return an empty list for unsupported resource type: deployments")
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
            logger.debug("Return an empty list for unsupported resource type: datacenters")
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
            logger.debug("Returning an empty list for unsupported resource type: subnets")
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
            logger.debug("Return an empty list for unsupported resource type: ssh_keys")
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
            logger.debug("Return an empty list for unsupported resource type: inputs")
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
            logger.debug("Return an empty list for unsupported resource type: instance_type")
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
    class RestClient {

        private ArrayList<Object> cookies;
        private String baseUrl
        private long lastAuthentication=0L;

        private def getReqCount = metrics.counter(MetricRegistry.name(RightscaleAPIRequest.class, "request.success"));
        private def authReqCount = metrics.counter(MetricRegistry.name(RightscaleAPIRequest.class, "authentication"));
        private def failReqCount = metrics.counter(MetricRegistry.name(RightscaleAPIRequest.class, "request.fail"));
        private def timer = metrics.timer(MetricRegistry.name(RightscaleAPIRequest, 'request.duration'))

        RestClient(baseUrl) {
            // API defaults
            Rest.defaultHeaders = ["X-API-VERSION": "1.5"]
            this.baseUrl = baseUrl
            Rest.baseUrl = this.baseUrl;

            ClientConfig cc = new DefaultClientConfig();
            cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, timeout);
            cc.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, timeout);
            Rest.client = Client.create(cc)
            addSendCookieFilter(Rest.client)
        }

        /**
         * Adds a filter to the jersey client that will send all of the stored cookies in each request.
         * @param client
         */
        private void addSendCookieFilter(Client client) {
            client.addFilter(new ClientFilter() {
                @Override
                public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
                    if (cookies != null) {
                        request.getHeaders().put("Cookie", cookies);
                    }
                    return getNext().handle(request);
                }
            });
        }

        /**
         * Login and create a session.
         */
        synchronized void authenticate() {
            logger.debug("Authenticating ${email}...")
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
            authReqCount.inc()
            lastAuthentication=System.currentTimeMillis()
            logger.debug("Successfully authenticated: ${email}")
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
        Node handleRequest(Closure makeRequest){
            def ClientResponse response = makeRequest()

            if (response.status == 403) {
                //authenticate if not re-authenticated in the last 30 seconds
                if(reauthenticate(30*1000)){
                    //retry request only if we are now authenticated again
                    response=makeRequest()
                    System.out.println("DEBUG: Reauthenticating to service.")
                    logger.debug("Reauthenticating to service.")

                }
            }
            if (response.status != 200) {
                failReqCount.inc()
                throw new RequestException("RightScale request error. " + response)
            }
            getReqCount.inc()

            return response.XML
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
            def time = timer.time()
            def long starttime = System.currentTimeMillis()
            System.out.println("DEBUG: Requesting resource href: ${href}.")
            logger.debug("Requesting resource href: ${href}.")

            /**
             * Request the servers data as XML
             */
            def request = new Rest(href);

            if (debug) {
                request.addFilter(new LoggingFilter(System.out)); // debug output
            }

            return handleRequest {


                def ClientResponse response = request.get([:], params)
                if (response.status == 422) {
                    // unsupported resource type
                    System.out.println("DEBUG: Unsupported resource type: ${href}")
                    logger.debug("Unsupported resource type: ${href}")
                    throw new UnsupportedResourceType("href: ${href}")
                }

                def endtime = System.currentTimeMillis()
                def duration = (endtime - starttime)
                System.out.println("DEBUG: Request succeeded: href ${href}. (duration=${duration})")
                logger.debug("Request succeeded: href ${href}. (duration=${duration})")
                time.stop()
                getReqCount.inc()
                return response
            }
        }

        Node post(String href, Map params, Map data) {
            def time = timer.time()
            def long starttime = System.currentTimeMillis()
            System.out.println("DEBUG: Requesting resource href: ${href}.")
            logger.debug("Requesting resource href: ${href}.")

            def request = new Rest(href);

            if (debug) {
                request.addFilter(new LoggingFilter(System.out)); // debug output
            }

            return handleRequest {
                def ClientResponse response = request.post({}, params, data)
                if (response.status == 422) {
                    // unsupported resource type
                    System.out.println("DEBUG: Unsupported resource type: ${href}")
                    logger.debug("Unsupported resource type: ${href}")
                    throw new UnsupportedResourceType("href: ${href}")
                }
                time.stop()
                def endtime = System.currentTimeMillis()
                def duration = (endtime - starttime)
                System.out.println("DEBUG: Request succeeded: href ${href}. (duration=${duration})")
                logger.debug("Request succeeded: href ${href}. (duration=${duration})")
                return response
            }
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

