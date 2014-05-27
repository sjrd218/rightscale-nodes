package com.simplifyops.rundeck.plugin.resources

import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException

/**
 *
 *
 * def r = new RightscaleResource()
 * r.links['self'] = '/api/blar/1'
 * r.attributes['state'] = 'operational'
 *
 * RightscaleResource.create(response.XML)
 * def node = new NodeEntryImpl()
 * r.populate(node)
 *
 */
class RightscaleResource {
    // creation time.
    def long ctime;
    // links to associated resources (keyed by their relationship name).
    def Map<String, String> links
    // set of attributes keyed by their name.
    def Map<String, String> attributes
    // prefix string for attribute key names
    def String prefix = ""
    // Character separating attribute name components
    def String attrSep = '.'

    void setPrefix(String s) {
        this.prefix = s;
    }

    RightscaleResource() {
        links = [:]
        attributes = [:]
        ctime = System.currentTimeMillis()
    }

    RightscaleResource(Node xmlNode) {
        this()
        xmlNode.links.link.each {
            links[it.'@rel'] = it.'@href'
        }
        attributes['name'] = xmlNode.name.text()
        attributes['description'] = xmlNode?.description.text()
        attributes['resource_uid'] = xmlNode?.resource_uid.text()
    }

    static RightscaleResource create(Node xmlNode) {
        return new RightscaleResource(xmlNode)
    }

    public String toString() {
        return links['self']
    }

    public String getId() {
        String href = links['self']
        if (null == href) throw new ResourceModelSourceException("Could not get id. resource does not have a link to self.")
        return href.split("/").last()
    }

    void populate(NodeEntryImpl node) {
        attributes.each { name, value ->
            node.setAttribute(generateAttributeName(name), value)
        }
    }

    String generateAttributeName(String name) {
        def keys = []
        if (null != prefix && !"".equals(prefix)) keys << prefix
        keys << name
        return keys.join(attrSep)
    }

    static void setTag(String tag, NodeEntryImpl node) {
        if (!node.tags.contains(tag)) node.tags.add(tag)
    }

    static Map burst(Node xmlNode, String key, Closure builder) {
        def results = [:]
        xmlNode[key].each {
            def RightscaleResource r = builder(it)
            if (r.links.containsKey('self')) {
                results[r.links['self']] = r

            } else {
                println("WARN: Skipping ${key} resource that does not have a link to self.")
            }
        }
        return results
    }
}

class CloudResource extends RightscaleResource {

    CloudResource() {
        super()
        setPrefix('cloud')
    }

    CloudResource(Node xmlNode) {
        super(xmlNode)
        setPrefix('cloud')
        attributes['cloud_type'] = xmlNode.cloud_type.text()
        attributes['display_name'] = xmlNode.display_name.text()
    }

    static CloudResource create(Node xmlNode) {
        return new CloudResource(xmlNode)
    }

    @Override
    void populate(NodeEntryImpl node) {
        super.populate(node)
        setTag(attributes['name'], node)
    }
}

class DeploymentResource extends RightscaleResource {

    DeploymentResource() {
        super()
        setPrefix('deployment')
    }

    DeploymentResource(Node xmlNode) {
        super(xmlNode)
        setPrefix('deployment')
    }

    static DeploymentResource create(Node xmlNode) {
        return new DeploymentResource(xmlNode)
    }

    @Override
    void populate(NodeEntryImpl node) {
        super.populate(node)
        setTag(attributes['name'], node)
    }
}

class DatacenterResource extends RightscaleResource {

    DatacenterResource() {
        super()
        setPrefix('datacenter')
    }

    DatacenterResource(Node xmlNode) {
        super(xmlNode)
        setPrefix('datacenter')
    }

    static DatacenterResource create(Node xmlNode) {
        return new DatacenterResource(xmlNode)
    }

    @Override
    void populate(NodeEntryImpl node) {
        super.populate(node)
        setTag(attributes['name'], node)
    }
}

class ImageResource extends RightscaleResource {

    ImageResource() {
        super()
        setPrefix('image')
    }

    ImageResource(Node xmlNode) {
        super(xmlNode)
        setPrefix('image')
        attributes['virtualization_type'] = xmlNode.virtualization_type.text()
        attributes['cpu_architecture'] = xmlNode.cpu_architecture.text()
        attributes['image_type'] = xmlNode.image_type.text()
        attributes['visibility'] = xmlNode.visibility.text()
        attributes['os_platform'] = xmlNode.os_platform.text()
    }

    static ImageResource create(Node xmlNode) {
        return new ImageResource(xmlNode)
    }
}

class InstanceTypeResource extends RightscaleResource {

    InstanceTypeResource() {
        super()
        setPrefix('instance_type')
    }

    InstanceTypeResource(Node xmlNode) {
        super(xmlNode)
        setPrefix('instance_type')
        attributes['virtualization_type'] = xmlNode.virtualization_type.text()
        attributes['cpu_architecture'] = xmlNode.cpu_architecture.text()
        attributes['local_disks'] = xmlNode.local_disks.text()
        attributes['local_disk_size'] = xmlNode.local_disk_size.text()
        attributes['memory'] = xmlNode.memory.text()
        attributes['cpu_speed'] = xmlNode.cpu_speed.text()
        attributes['cpu_count'] = xmlNode.cpu_count.text()
    }

    static InstanceTypeResource create(Node xmlNode) {
        return new InstanceTypeResource(xmlNode)
    }
}

class InstanceResource extends RightscaleResource {

    InstanceResource() {
        super()
        setPrefix('instance')
    }

    InstanceResource(Node xmlNode) {
        super(xmlNode)
        setPrefix('instance')
        attributes['terminated_at'] = xmlNode.terminated_at.text()
        attributes['public_ip_address'] = xmlNode.public_ip_addresses.public_ip_address.text()
        attributes['private_ip_address'] = xmlNode.private_ip_addresses.private_ip_address.text()
        attributes['public_dns_name'] = xmlNode.public_dns_names.public_dns_name.text()
        attributes['private_dns_name'] = xmlNode.private_dns_names.private_dns_name.text()
        attributes['monitoring_id'] = xmlNode.monitoring_id.text()
        attributes['os_platform'] = xmlNode.os_platform.text()
        attributes['monitoring_server'] = xmlNode.monitoring_server.text()
        attributes['security_groups'] = xmlNode.security_groups.text()
        attributes['user_data'] = xmlNode.user_data.text()
        attributes['pricing_type'] = xmlNode.pricing_type.text()
        attributes['subnets'] = xmlNode.subnets.text()
        attributes['state'] = xmlNode.state.text()
        attributes['created_at'] = xmlNode.created_at.text()
        attributes['updated_at'] = xmlNode.updated_at.text()
    }


    static InstanceResource create(Node xmlNode) {
        return new InstanceResource(xmlNode)
    }

    @Override
    void populate(NodeEntryImpl node) {
        super.populate(node)
        node.setHostname(attributes['public_ip_address']) // TODO: Convention agreement.
    }
}

class ServerResource extends RightscaleResource {
    def cloud

    ServerResource() {
        super()
        setPrefix('server')
    }

    ServerResource(Node xmlNode) {
        super(xmlNode)
        setPrefix('server')
        attributes['state'] = xmlNode.state.text()
        attributes['created_at'] = xmlNode.created_at.text()
        attributes['updated_at'] = xmlNode.updated_at.text()
        /*
         * Generate a link to the cloud. Get it from the current_instance.
         */
        def cloud_href = xmlNode.current_instance.links.link.find { it.'@rel' == 'cloud' }?.'@href'
        if (null != cloud_href) links['cloud'] = cloud_href
        cloud = cloud_href
    }

    static RightscaleResource create(Node xmlNode) {
        return new ServerResource(xmlNode)
    }

    @Override
    void populate(NodeEntryImpl node) {
        super.populate(node)
        node.setNodename(attributes['name'])
    }
}

class ServerArrayResource extends RightscaleResource {

    ServerArrayResource() {
        super()
        setPrefix('server_array')
    }

    ServerArrayResource(Node xmlNode) {
        super(xmlNode)
        setPrefix('server_array')
        attributes['instances_count'] = xmlNode.instances_count.text()
        attributes['array_type'] = xmlNode.array_type.text()
        attributes['min_count'] = xmlNode.elasticity_params.bounds.min_count.text()
        attributes['max_count'] = xmlNode.elasticity_params.bounds.max_count.text()
        attributes['state'] = xmlNode.state.text()
    }

    static ServerArrayResource create(Node xmlNode) {
        return new ServerArrayResource(xmlNode)
    }

    @Override
    void populate(NodeEntryImpl node) {
        super.populate(node)
        setTag(attributes['name'], node)
    }
}

class ServerTemplateResource extends RightscaleResource {


    ServerTemplateResource() {
        super()
        setPrefix('server_template')
    }

    ServerTemplateResource(Node xmlNode) {
        super(xmlNode)
        setPrefix('server_template')
        attributes['revision'] = xmlNode.revision.text()
    }

    static ServerTemplateResource create(Node xmlNode) {
        return new ServerTemplateResource(xmlNode)
    }
}

class SshKeyResource extends RightscaleResource {

    SshKeyResource() {
        super()
        setPrefix('ssh_key')
    }

    SshKeyResource(Node xmlNode) {
        super(xmlNode)
        setPrefix('ssh_key')
    }

    static SshKeyResource create(Node xmlNode) {
        return new SshKeyResource(xmlNode)
    }
}

class SubnetResource extends RightscaleResource {

    SubnetResource() {
        super()
        setPrefix('subnet')
    }

    SubnetResource(Node xmlNode) {
        super(xmlNode)
        setPrefix('subnet')
        attributes['visibility'] = xmlNode.visibility.text()
        attributes['is_default'] = xmlNode.is_default.text()
        attributes['cidr_block'] = xmlNode.cidr_block.text()
        attributes['state'] = xmlNode.state.text()
    }

    static SubnetResource create(Node xmlNode) {
        return new SubnetResource(xmlNode)
    }
}

class InputResource extends RightscaleResource {

    InputResource() {
        super()
        setPrefix('inputs')
    }

    InputResource(Node xmlNode) {
        super(xmlNode)
        setPrefix('inputs')
        attributes['value'] = xmlNode.value.text()
    }

    static InputResource create(Node xmlNode) {
        return new InputResource(xmlNode)
    }

    @Override
    void populate(NodeEntryImpl node) {
        // Replace slashes with dots.
        def attrName = generateAttributeName(attributes['name'].replace('/', '.'))
        // set the node attribute.
        node.setAttribute(attrName, attributes['value'])
    }
}

class TagsResource extends RightscaleResource {

    TagsResource() {
        super()
        setPrefix('tags')
    }

    TagsResource(Node xmlNode) {
        this()
        xmlNode.links.link.each {
            links[it.'@rel'] = it.'@href'
        }
        // make resource a synonym for self so #burst() function can find it.
        links['self'] = links['resource']
        /**
         * Flatten tag elements into a comma separated list.
         */
        def tags = []
        xmlNode.tags.tag.each {
            tags << it.name.text()
        }
        attributes['tags'] = tags.join(',')
    }

    static TagsResource create(Node xmlNode) {
        return new TagsResource(xmlNode)
    }


    @Override
    void populate(NodeEntryImpl node) {
        super.populate(node)
        attributes['tags'].split(",").each {
            setTag(it, node)
        }
        node.setAttribute('resource_tags',attributes['tags'])
    }

    public String toString() {
        return attributes['tags']
    }

    static boolean hasAttributeForm(String name) {
        def m = name =~ /([^=]+)=(.*)/
        return m.matches()
    }

     void setAttribute(String input, NodeEntryImpl node) {
        def m = input =~ /([^=]+)=(.*)/
        if (m.matches()) {
            def String name = m[0][1]
            def String value = m[0][2]
            node.setAttribute(generateAttributeName(name), value)
        }
    }
}