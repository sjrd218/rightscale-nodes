package com.simplifyops.rundeck.plugin.resources

import com.dtolabs.rundeck.core.resources.ResourceModelSourceException
import com.sun.jersey.api.client.ClientHandlerException
import com.sun.jersey.api.client.ClientRequest
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.sun.jersey.api.client.filter.ClientFilter
import com.sun.jersey.api.client.filter.LoggingFilter
import com.sun.jersey.api.representation.Form
import org.apache.log4j.Logger;
import groovyx.gpars.GParsPool

class RightscaleQuery {
    static Logger logger = Logger.getLogger(RightscaleNodes.class);

    Map servers = [:]
    Map instances = [:]
    Map deployments = [:]
    Map clouds = [:]
    Map datacenters = [:]
    Map subnets = [:]
    Map server_templates = [:]
    Map images = [:]
    Map instance_types = [:]
    Map tags = [:]
    Map server_arrays = [:]
    Map server_array_instances = [:]
    Map resource_inputs = [:]

    /**
     * Default constructor.
     */
    RightscaleQuery(String email, String password, String account, String endpoint) {

        // API defaults
        Rest.defaultHeaders = ["X-API-VERSION": "1.5"]
        Rest.baseUrl = endpoint;

        // Login to the service
        authenticate(email, password, account, endpoint)

        // Make an initial query to populate data that doesn't change often.
        initialize()
    }

    /**
     * Login and create a session.
     */
    private void authenticate(String email, String password, String account, String endpoint) {
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
        if (response.status != 204) {
            throw new ResourceModelSourceException("RightScale login error. " + response)
        }
    }

    /**
     * Initialize data objects that don't change much.
     */
    private void initialize() {
        // clouds
        GParsPool.withPool {
            def getCloudResources = { cloud_id ->
                GParsPool.executeAsyncAndWait(
                        { requestDatacenters(cloud_id) },
                        { requestInstances(cloud_id) },
                        { requestInstanceTypes(cloud_id) })
            }
            // Get the resources for each of the clouds
            requestClouds().each { href, model ->
                getCloudResources.callAsync(model.cloud_id)
            }
        }

        GParsPool.withPool {
            GParsPool.executeAsyncAndWait(
                    { requestDeployments() },
                    { requestServerTemplates() })
        }

    }

    public Map listServers() {
        if (servers.size() == 0) {
            servers = requestServers()
        }
        return servers;
    }

/**
 * Query API service for the Servers
 * @return Map mof models keyed by server href.
 */
    private Map requestServers() {
        def Map resources = [:]
        def Node root = new RightscaleRequest().get("/api/servers.xml", [view: "instance_detail"])
        System.out.println("DEBUG: number of servers in response to GET /api/servers: " + root.server.size())
        /**
         * Traverse the dom to get each of the server nodes
         */
        root.server.each { svr ->
            def svr_href = svr.links.link.find { it.'@rel' == 'self' }?.'@href'
            System.out.println("DEBUG: server: " + svr_href)

            /**
             * If it doesn't have a name, ignore it.
             */
            if (null != svr.name.text()) {
                // Also, ignore servers that don't have an instance.
                def current_instance = svr.links.link.find { it.'@rel' == 'current_instance' }?.'@href'
                if (null != current_instance) {
                    def model = [:]
                    model.href = svr_href
                    model.name = svr.name.text()
                    model.description = svr.description.text()
                    model.state = svr.state.text()
                    model.created_at = svr.created_at.text()
                    model.deployment_href = svr.links.link.find { it.'@rel' == 'deployment' }?.'@href'
                    model.current_instance_href = current_instance
                    model.next_instance_href = svr.links.link.find { it.'@rel' == 'next_instance' }?.'@href'
                    model.alerts_href = svr.links.link.find { it.'@rel' == 'alerts' }?.'@href'
                    model.alerts_spec_href = svr.links.link.find { it.'@rel' == 'alert_specs' }?.'@href'

                    resources.put(model.href, model)
                } else {
                    System.println("DEBUG: Skipping over server with null current_instance")
                }
            } else {
                System.println("DEBUG: Skipping over server with a null name")
            }
        }
        System.out.println("DEBUG: number of servers returned in map: " + resources.size())

        return resources
    }

/**
 * Query API service for a list of instances for the cloud.
 * @param cloud_id
 * @return Map of models keyed by cloud href.
 */
    private Map requestInstances(String cloud_id) {
        def Node root = new RightscaleRequest().get("/api/clouds/${cloud_id}/instances", [view: "extended"])
        root.instance.each {
            Map model = parseInstance(it)
            model.href = root.instance.links.link.find { it.'@rel' == 'self' }?.'@href'
            instances.put(model.href, model)
        }
        return instances
    }

/**
 * Query API service for a single Instance
 * @param instance_href
 * @return a Model of the instance
 */
    private Map requestInstance(String instance_href) {

        def Node instance = new RightscaleRequest().get(instance_href, [view: "extended"])

        Map model = parseInstance(instance)

        instances.put(instance_href, model)

        return instances.get(instance_href)
    }

/**
 * Parse the XML node and populate a Model map.
 */
    private Map parseInstance(Node instance) {
        def model = [:]
        model.href = instance.links.link.find { it.'@rel' == 'self' }?.'@href'
        model.name = instance.name.text()
        model.resource_uid = instance.resource_uid.text()
        model.public_ip_address = instance?.public_ip_addresses?.public_ip_address[0]?.text()
        model.private_ip_address = instance?.private_ip_addresses?.private_ip_address[0]?.text()
        model.public_dns_name = instance.public_dns_names?.public_dns_name[0]?.text()
        model.private_dns_name = instance.private_dns_names?.private_dns_name[0]?.text()
        model.user_data = instance.user_data.text()
        model.server_template_href = instance.links.link.find { it.'@rel' == 'server_template' }?.'@href'
        model.cloud_href = instance.links.link.find { it.'@rel' == 'cloud' }?.'@href'
        model.datacenter_href = instance.links.link.find { it.'@rel' == 'datacenter' }?.'@href'
        model.deployment_href = instance.links.link.find { it.'@rel' == 'deployment' }?.'@href'
        model.subnet_href = instance?.subnets?.subnet[0]?.@href
        model.image_href = instance.links.link.find { it.'@rel' == 'image' }?.'@href'
        model.instance_type_href = instance.links.link.find { it.'@rel' == 'instance_type' }?.'@href'
        return model
    }

/**
 * Retrieve an Instance
 * @param href The href to the instance
 * @return an instance Model map.
 */
    public Map getInstance(String href) {
        if (!instances.containsKey(href)) {
            requestInstance(href)
        }
        return instances.get(href)
    }

/**
 * Get a deployment
 * @param href The href to the deployment
 * @return a deployment Model map.
 */
    public Map getDeployment(String href) {
        return listDeployments().get(href)
    }

/**
 * List all the deployments
 * @return
 */
    public Map listDeployments() {
        if (deployments.size() == 0) {
            deployments = requestDeployments()
        }
        return deployments
    }

/**
 * Query Rightscale API service for all the deployments.
 * @return Map of Deployments keyed by their href
 */
    private Map requestDeployments() {
        def Map resources = [:]

        def Node root = new RightscaleRequest().get("/api/deployments.xml", [:])
        root.deployment.each {
            def href = it.links.link.find { it.'@rel' == 'self' }?.'@href'
            def model = [:]
            model.href = href
            model.name = it.name.text()

            resources.put(href, model)
            System.out.println("DEBUG: added deployment " + model.name);
        }
        System.out.println("DEBUG: Number deployment resources found: " + resources.size())
        return resources;
    }

/**
 * Query the Rightscale API for the tags assigned to the specified resource.
 * @return Map of Tags
 */
    private Collection requestTags(final String href) {
        def results = []
        def request = '/api/tags/by_resource.xml' as Rest;
        request.addFilter(new LoggingFilter(System.out)); // debug output
        def ClientResponse response = request.post({}, [:], ["resource_hrefs[]": href]);
        if (response.status != 200) {
            throw new ResourceModelSourceException("RightScale ${href} tags request error. " + response)
        }
        def Node resource_tags = response.XML
        resource_tags.resource_tag.tags.tag.each {
            results << it.name.text()
        }
        tags.put(href, results)
        System.out.println("DEBUG: Number tags found: " + results.size())
        return results;
    }

    Collection getTags(final String href) {
        if (!tags.containsKey(href)) {
            requestTags(href)
        }
        return tags.get(href)
    }

    Map getCloud(String href) {
        if (clouds.size() == 0) {
            clouds = requestClouds()
        }
        return (Map) clouds.get(href)
    }

/**
 * Query the Rightscale API for the Cloud resources.
 * @return Map of Clouds
 */
    private Map requestClouds() {

        def Node root = new RightscaleRequest().get("/api/clouds.xml", [:])
        root.cloud.each {
            def href = it.links.link.find { it.'@rel' == 'self' }?.'@href'
            def model = [:]
            model.href = href
            model.name = it.name.text()
            model.description = it.description.text()
            model.cloud_type = it.cloud_type.text()
            model.cloud_id = href.split("/").last()
            // get the link relations
            model.datacenters_href = it.links.link.find { it.'@rel' == 'datacenters' }?.'@href'
            model.instance_types_href = it.links.link.find { it.'@rel' == 'instance_types' }?.'@href'
            model.security_groups_href = it.links.link.find { it.'@rel' == 'security_groups' }?.'@href'
            model.instances_href = it.links.link.find { it.'@rel' == 'instances' }?.'@href'
            model.images_href = it.links.link.find { it.'@rel' == 'images' }?.'@href'
            model.ip_addresses_href = it.links.link.find { it.'@rel' == 'ip_addresses' }?.'@href'
            model.ip_address_bindings_href = it.links.link.find { it.'@rel' == 'ip_address_bindings' }?.'@href'
            model.volume_attachments_href = it.links.link.find { it.'@rel' == 'volume_attachments' }?.'@href'
            model.recurring_volume_attachments_href = it.links.link.find { it.'@rel' == 'recurring_volume_attachments' }?.'@href'
            model.ssh_keys_href = it.links.link.find { it.'@rel' == 'ssh_keys' }?.'@href'
            model.volume_snapshots_href = it.links.link.find { it.'@rel' == 'volume_snapshots' }?.'@href'
            model.volume_types_href = it.links.link.find { it.'@rel' == 'volume_types' }?.'@href'
            model.volume_href = it.links.link.find { it.'@rel' == 'volume' }?.'@href'

            clouds.put(model.href, model)
            System.out.println("DEBUG: added cloud " + model.name);
        }
        return clouds;
    }

    Map getServerTemplate(final String href) {
        if (!server_templates.containsKey(href)) {
            requestServerTemplate(href)
        }
        return server_templates.get(href)
    }

/**
 * Query the Rightscale API for a single ServerTemplate resources.
 * @return Model for the ServerTemplate
 */
    private Map requestServerTemplate(final String href) {

        def Node root = new RightscaleRequest().get(href + '.xml', [:])

        def ref = root.links.link.find { it.'@rel' == 'self' }?.'@href'
        def model = [:]
        model.href = ref
        model.name = root.name.text()
        model.description = root.description.text()
        model.revision = root.revision.text()
        // get the link relations
        model.multi_cloud_images_href = root.links.link.find { it.'@rel' == 'multi_cloud_images' }?.'@href'
        model.default_multi_cloud_image_href = root.links.link.find { it.'@rel' == 'default_multi_cloud_image' }?.'@href'
        model.inputs_href = root.links.link.find { it.'@rel' == 'inputs' }?.'@href'
        model.publication_href = root.links.link.find { it.'@rel' == 'publication' }?.'@href'
        model.alert_specs_href = root.links.link.find { it.'@rel' == 'alert_specs' }?.'@href'
        model.runnable_bindings_href = root.links.link.find { it.'@rel' == 'runnable_bindings' }?.'@href'
        model.cookbook_attachments_href = root.links.link.find { it.'@rel' == 'cookbook_attachments' }?.'@href'

        System.out.println("DEBUG: ServerTemplate: " + model.name);
        server_templates.put(href, model)

        return (Map) server_templates.get(href)
    }

/**
 * Query the Rightscale API for all ServerTemplate resources.
 * @return
 */
    private Map requestServerTemplates() {
        def Node root = new RightscaleRequest().get('/api/server_templates.xml', [:])
        root.server_template.each { server_template ->
            def ref = server_template.links.link.find { it.'@rel' == 'self' }?.'@href'
            def model = [:]
            model.href = ref
            model.name = server_template.name.text()
            model.description = server_template.description.text()
            model.revision = server_template.revision.text()
            // get the link relations
            model.multi_cloud_images_href = server_template.links.link.find { it.'@rel' == 'multi_cloud_images' }?.'@href'
            model.default_multi_cloud_image_href = server_template.links.link.find { it.'@rel' == 'default_multi_cloud_image' }?.'@href'
            model.inputs_href = server_template.links.link.find { it.'@rel' == 'inputs' }?.'@href'
            model.publication_href = server_template.links.link.find { it.'@rel' == 'publication' }?.'@href'
            model.alert_specs_href = server_template.links.link.find { it.'@rel' == 'alert_specs' }?.'@href'
            model.runnable_bindings_href = server_template.links.link.find { it.'@rel' == 'runnable_bindings' }?.'@href'
            model.cookbook_attachments_href = server_template.links.link.find { it.'@rel' == 'cookbook_attachments' }?.'@href'

            System.out.println("DEBUG: ServerTemplate: " + model.name);
            server_templates.put(model.href, model)

        }
        return server_templates
    }

    Map getDatacenter(String href) {
        if (!datacenters.containsKey(href)) {
            requestDatacenter(href)
        }
        return datacenters.get(href)
    }

/**
 * Query the Rightscale API for a single Datacenter resource.
 * * @return Model of Datacenter
 */
    private Map requestDatacenter(final String href) {
        def model = [:]

        def Node datacenter = new RightscaleRequest().get(href + '.xml', [:])
        def ref = datacenter.links.link.find { it.'@rel' == 'self' }?.'@href'
        model.href = ref
        model.name = datacenter.name.text()
        model.description = datacenter.description.text()
        model.resource_uid = datacenter.resource_uid.text()
        // get the link relations
        model.cloud_href = datacenter.links.link.find { it.'@rel' == 'cloud' }?.'@href'

        datacenters.put(model.href, model)
        System.out.println("DEBUG: added: " + model.name);

        return model;
    }

/**
 * Query the Rightscale API for all Datacenter resources for the specified cloud.
 * @param cloud_id
 * @return
 */
    private Map requestDatacenters(String cloud_id) {

        def Node root = new RightscaleRequest().get("/api/clouds/${cloud_id}/datacenters.xml", [:])
        root.datacenter.each { datacenter ->
            def model = [:]
            def ref = datacenter.links.link.find { it.'@rel' == 'self' }?.'@href'
            model.href = ref
            model.name = datacenter.name.text()
            model.description = datacenter.description.text()
            model.resource_uid = datacenter.resource_uid.text()
            // get the link relations
            model.cloud_href = datacenter.links.link.find { it.'@rel' == 'cloud' }?.'@href'
            model.cloud_id = cloud_id
            datacenters.put(model.href, model)
            System.out.println("DEBUG: added: " + model.name);
        }
        return datacenters
    }

    Map getSubnet(String href) {
        if (!subnets.containsKey(href)) {
            requestSubnet(href)
        }
        return subnets.get(href)
    }

/**
 * Query the Rightscale API for a single Subnet resource.
 * @return Map of Subnets
 */
    private Map requestSubnet(final String href) {
        def model = [:]

        def Node subnet = new RightscaleRequest().get(href + '.xml', [:])

        def ref = subnet.links.link.find { it.'@rel' == 'self' }?.'@href'
        model.href = ref
        model.name = subnet.name.text()
        model.description = subnet.description.text()
        model.state = subnet.state.text()
        model.resource_uid = subnet.resource_uid.text()
        model.cidr_block = subnet.cidr_block.text()
        model.is_default = subnet.is_default.text()
        model.visibility = subnet.visibility.text()
        // get the link relations
        model.datacenter_href = subnet.links.link.find { it.'@rel' == 'datacenter' }?.'@href'
        model.network_href = subnet.links.link.find { it.'@rel' == 'network' }?.'@href'

        subnets.put(model.href, model)
        System.out.println("DEBUG: added: " + model.name);

        return model;
    }

    Map getImage(String href) {
        if (!images.containsKey(href)) {
            requestImage(href)
        }

        return images.get(href)
    }

/**
 * Query the Rightscale API for a single Image resource.
 * @return Map of Images
 */
    private Map requestImage(final String href) {
        def model = [:]

        def Node image = new RightscaleRequest().get(href + '.xml', [:])
        def ref = image.links.link.find { it.'@rel' == 'self' }?.'@href'
        model.href = ref
        model.name = image.name.text()
        model.description = image.description.text()
        model.cpu_architecture = image.cpu_architecture.text()
        model.image_type = image.image_type.text()
        model.virtualization_type = image.virtualization_type.text()
        model.os_platform = image.os_platform.text()

        images.put(model.href, model)
        System.out.println("DEBUG: added: " + model.name);

        return model;
    }

/**
 * Query the Rightscale API for all ResourceInput resources for the specified instance.
 * @param cloud_id
 * @param instance_id
 * @return List of maps containing name/value pairs.
 */
    private Collection requestResourceInputs(final String cloud_id, String instance_id) {
        if (!resource_inputs.containsKey("${cloud_id}/${instance_id}")) {
            System.out.println("DEBUG: Getting inputs for ${cloud_id}/${instance_id}")
            def inputs = []
            //  /api/clouds/:cloud_id/instances/:instance_id/inputs
            def href = "/api/clouds/${cloud_id}/instances/${instance_id}/inputs"

            def Node root = new RightscaleRequest().get(href + '.xml', [:])
            root.input.each { input ->
                def model = [:]
                model.name = input.name.text()
                model.value = input.value.text()
                inputs.add(model)
            }
            resource_inputs.put("${cloud_id}/${instance_id}", inputs)
        }

        return resource_inputs.get("${cloud_id}/${instance_id}")
    }

    Collection getResourceInputs(final String cloud_id, String instance_id) {
        return requestResourceInputs(cloud_id, instance_id)
    }

    Map getInstanceType(String href) {
        if (!instance_types.containsKey(href)) {
            requestInstanceType(href)
        }
        return instance_types.get(href)
    }
/**
 * Query the Rightscale API for a single InstanceType resources.
 * @return Model of the Instance_type
 */
    private Map requestInstanceType(final String href) {
        def model = [:]

        def Node instance_type = new RightscaleRequest().get(href + '.xml', [:])

        def ref = instance_type.links.link.find { it.'@rel' == 'self' }?.'@href'
        model.href = ref
        model.name = instance_type.name.text()
        model.description = instance_type.description.text()
        model.memory = instance_type.memory.text()
        model.cpu_architecture = instance_type.cpu_architecture.text()
        model.local_disks = instance_type.local_disks.text()
        model.local_disk_size = instance_type.local_disk_size.text()
        model.cpu_count = instance_type.cpu_count.text()
        model.cpu_speed = instance_type.cpu_speed.text()
        model.cloud_href = instance_type.links.link.find { it.'@rel' == 'cloud' }?.'@href'
        System.out.println("DEBUG: added: " + model.name);
        instance_types.put(model.href, model)
        return model;
    }
/**
 * Query the Rightscale API for all InstanceType resources for the specified cloud.
 * @param cloud_id
 * @return
 */
    private Map requestInstanceTypes(String cloud_id) {
        def Node root = new RightscaleRequest().get("/api/clouds/${cloud_id}/instance_types.xml", [:])
        root.instance_type.each { instance_type ->
            def model = [:]
            def ref = instance_type.links.link.find { it.'@rel' == 'self' }?.'@href'
            model.href = ref
            model.name = instance_type.name.text()
            model.description = instance_type.description.text()
            model.memory = instance_type.memory.text()
            model.cpu_architecture = instance_type.cpu_architecture.text()
            model.local_disks = instance_type.local_disks.text()
            model.local_disk_size = instance_type.local_disk_size.text()
            model.cpu_count = instance_type.cpu_count.text()
            model.cpu_speed = instance_type.cpu_speed.text()
            model.cloud_href = instance_type.links.link.find { it.'@rel' == 'cloud' }?.'@href'
            System.out.println("DEBUG: added: " + model.name);
            instance_types.put(model.href, model)
        }
        return instance_types
    }

/**
 * Query the Rightscale API for all ServerArray resources.
 * @return
 */
    private Map requestServerArrays() {
        def Node root = new RightscaleRequest().get("/api/server_arrays", [view: "instance_detail"])
        root.server_array.each {
            def href = it.links.link.find { it.'@rel' == 'self' }?.'@href'
            def model = [:]
            model.href = href
            model.name = it.name.text()
            model.description = it.description.text()
            model.state = it.state.text()
            model.instances_count = it.instances_count.text()
            model.current_instances = it.links.link.find { it.'@rel' == 'current_instances' }?.'@href'
            model.deployment = it.links.link.find { it.'@rel' == 'deployment' }?.'@href'

            server_arrays.put(href, model)
        }
        return server_arrays
    }

    Map getServerArrays() {
        if (server_arrays.size() == 0) {
            server_arrays = requestServerArrays()
        }
        return server_arrays;
    }

/**
 * Query the Rightscale API for all Instances resources for the specified ServerArray.
 * @param server_array_href
 * @return
 */
    private Map requestServerArrayInstances(String server_array_href) {
        Map results = [:]

        def server_array_id = server_array_href.split("/").last()
        def Node instances = new RightscaleRequest().get("/api/server_arrays/${server_array_id}/current_instances", [view: "extended"])
        instances.instance.each {
            System.out.println("DEBUG: server_array instance: " + it)

            Map instance = parseInstance(it)
            instance.server_array_href = server_array_href
            instance.server_array = server_arrays.get(server_array_href).name
            results.put(instance.href, instance)
        }

        server_array_instances.put(server_array_href, results)

        return results;
    }

    Map listServerArrayInstances() {
        def results = [:]
        getServerArrays().each { href, model ->
            if (!server_array_instances.containsKey(href)) {
                requestServerArrayInstances(href)
            }
            Map instances = server_array_instances.get(href)
            results.putAll(instances)
        }
        return results
    }

}

/**
 * Helper class for making Rightscale API requests.
 */
class RightscaleRequest {
    /**
     * Get the resource
     * @param href
     * @param params
     * @return Node containing the resource as XML data.
     */
    Node get(String href, Map params) {
        if (null == href) throw new IllegalAccessException("href cannot be null")

        System.out.println("DEBUG: RightscaleRequest: Getting resource by href: ${href}")
        /**
         * Request the servers data as XML
         */
        def request = href as Rest;
        request.addFilter(new LoggingFilter(System.out)); // debug output
        def ClientResponse response = request.get([:], params); // instance_detail contains extra info
        if (response.status != 200) {
            throw new ResourceModelSourceException("RightScale ${href} request error. " + response)
        }

        return response.XML
    }

    Node post(String href, Map params) {
        def request = href as Rest;

        request.addFilter(new LoggingFilter(System.out)); // debug output
        def ClientResponse response = request.post({}, [:], params);
        if (response.status != 200) {
            throw new ResourceModelSourceException("RightScale ${href} request error. " + response)
        }

        return response.XML
    }
}
