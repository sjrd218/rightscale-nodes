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

    /**
     * Default constructor.
     */
    RightscaleQuery(String email, String password, String account, String endpoint) {
        /*
      * Setup API defaults
      */
        Rest.defaultHeaders = ["X-API-VERSION": "1.5"]
        Rest.baseUrl = endpoint;

        authenticate(email, password, account, endpoint)

    }

    /**
     * Login and create a session
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

    public Map listServers() {
        if (servers.size() == 0) {
            servers = requestServers()
        }
        return servers;
    }

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


    private Map requestInstance(String instance_href) {

        def Node instance = new RightscaleRequest().get(instance_href, [view: "extended"])

        Map model = parseInstance(instance)

        instances.put(instance_href, model)

        return instances.get(instance_href)
    }

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

    public Map getInstance(String href) {
        if (!instances.containsKey(href)) {
            requestInstance(href)
        }
        return instances.get(href)
    }

    public Map getDeployment(String href) {
        return listDeployments().get(href)
    }

    public Map listDeployments() {
        if (deployments.size() == 0) {
            deployments = requestDeployments()
        }
        return deployments
    }

    /**
     *
     * @return Map of Deployments
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
     *
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
     *
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
     *
     * @return Map of ServerTemplates
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

    Map getDatacenter(String href) {
        if (!datacenters.containsKey(href)) {
            requestDatacenter(href)
        }
        return datacenters.get(href)
    }

    /**
     *
     * @return Map of Datacenters
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

    Map getSubnet(String href) {
        if (!subnets.containsKey(href)) {
            requestSubnet(href)
        }
        return subnets.get(href)
    }

    /**
     *
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
     *
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

    Map getInstanceType(String href) {
        if (!instance_types.containsKey(href)) {
            requestInstanceType(href)
        }
        return instance_types.get(href)
    }
    /**
     *
     * @return Map of Instance_types
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
