package com.simplifyops.rundeck.plugin.resources

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class RightscaleNodesTest {

    /**
     * Populate the cache with test data.
     * @param cache
     */
    private void initializeCache(RightscaleBasicCache cache) {
        cache.updateClouds(
                CloudResource.burst(new XmlParser().parseText(XmlData.CLOUDS),
                        'cloud', CloudResource.&create))
        cache.updateDatacenters(
                DatacenterResource.burst(new XmlParser().parseText(XmlData.DATACENTERS),
                        'datacenter', DatacenterResource.&create))
        cache.updateDeployments(
                DeploymentResource.burst(new XmlParser().parseText(XmlData.DEPLOYMENTS),
                        'deployment', DatacenterResource.&create))
        cache.updateImages(
                ImageResource.burst(new XmlParser().parseText(XmlData.IMAGES),
                        'image', ImageResource.&create))
        cache.updateInputs(
                InputResource.burst(new XmlParser().parseText(XmlData.INPUTS),
                        'input', InputResource.&create))
        cache.updateInstances(
                InstanceResource.burst(new XmlParser().parseText(XmlData.INSTANCES),
                        'instance', InstanceResource.&create))
        cache.updateInstanceTypes(
                InstanceTypeResource.burst(new XmlParser().parseText(XmlData.INSTANCE_TYPES),
                        'instance_type', InstanceTypeResource.&create))
        cache.updateServerArrays(
                ServerArrayResource.burst(new XmlParser().parseText(XmlData.SERVER_ARRAYS),
                        'server_array', ServerArrayResource.&create))
        cache.updateServerTemplates(
                ServerTemplateResource.burst(new XmlParser().parseText(XmlData.SERVER_TEMPLATES),
                        'server_template', ServerTemplateResource.&create))
        cache.updateServers(
                ServerResource.burst(new XmlParser().parseText(XmlData.SERVERS),
                        'server', ServerResource.&create))
        cache.updateSshKeys(
                SshKeyResource.burst(new XmlParser().parseText(XmlData.SSH_KEYS),
                        'ssh_key', SshKeyResource.&create))
        cache.updateSubnets(
                SubnetResource.burst(new XmlParser().parseText(XmlData.SUBNETS),
                        'subnet', SubnetResource.&create))
        cache.updateTags(
                TagsResource.burst(new XmlParser().parseText(XmlData.TAGS),
                        'tag', TagsResource.&create))
    }


    @Test
    public void cacheRefresh() {
        def RightscaleBasicCache cache = new RightscaleBasicCache()
        initializeCache(cache)
    }

    @Test
    public void constructor() {
        RightscaleNodes rightscaleNodes = new RightscaleNodes(createConfigProperties())
        Assert.assertNotNull(rightscaleNodes)
    }

    private Properties createConfigProperties() {
        def configuration = new Properties()
        configuration.setProperty(RightscaleNodesFactory.EMAIL, "admin@acme.com")
        configuration.setProperty(RightscaleNodesFactory.PASSWORD, "password")
        configuration.setProperty(RightscaleNodesFactory.ACCOUNT, "12345")
        configuration.setProperty(RightscaleNodesFactory.ENDPOINT, "http://my.rightscale.com")
        configuration.setProperty(RightscaleNodesFactory.USERNAME, "admin")
        configuration.setProperty(RightscaleNodesFactory.REFRESH_INTERVAL, "30")
        configuration.setProperty(RightscaleNodesFactory.INPUT_PATT, ".*")
        configuration.setProperty(RightscaleNodesFactory.TAG_PATT, ".*")
        configuration.setProperty(RightscaleNodesFactory.TAG_ATTR, "true")
        configuration.setProperty(RightscaleNodesFactory.METRICS_INTVERVAL, "1")
        configuration.setProperty(RightscaleNodesFactory.HTTP_TIMEOUT, "30000")
        configuration.setProperty(RightscaleNodesFactory.HTTP_LOG, "false")
        configuration.setProperty(RightscaleNodesFactory.ALL_RESOURCES,"true")
        return configuration
    }

    @Test
    public void createNode() {
        RightscaleNodes rightscaleNodes = new RightscaleNodes(createConfigProperties())
        def node = rightscaleNodes.createNode('node1')
        Assert.assertEquals("unix", node.getOsFamily())
        Assert.assertEquals("admin", node.getUsername())
    }

    @Test
    public void validate() {
        def configuration = new Properties()
        try {
            new RightscaleNodes(configuration).validate()
        } catch (ConfigurationException e) {
            // supposed to throw an exception.
        }
        new RightscaleNodes(createConfigProperties()).validate()
    }

    @Test
    public void getServerNodes() {
        def RightscaleBasicCache cache = new RightscaleBasicCache()
        initializeCache(cache)
        def rightscaleNodes = new RightscaleNodes(createConfigProperties())

        def nodeSet = rightscaleNodes.populateServerNodes(cache)

        Assert.assertEquals(2, nodeSet.getNodes().size())
        def nodenames = nodeSet.getNodeNames()

        def node1name = "name_1680878585 resource_3580826380"
        def node2name = "name_642936808 resource_369673713"
        Assert.assertTrue("\"${node1name}\" node not found. nodes:" + nodenames, nodenames.contains(node1name))
        Assert.assertTrue("\"${node2name}\" node not found. nodes:" + nodenames, nodenames.contains(node2name))

        def INodeEntry node1 = nodeSet.getNodes().toArray()[0]
        def node1Attrs = node1.getAttributes()
        // server
        Assert.assertEquals("attrs" + node1Attrs, "operational", node1Attrs['server.state'])
        Assert.assertEquals("name_2149427003", node1Attrs['server.name'])
        Assert.assertEquals("description_3746459714", node1Attrs['server.description'])
        Assert.assertEquals("2013/10/02 01:38:50 +0000", node1Attrs['server.created_at'])
        Assert.assertEquals("2013/10/02 01:38:50 +0000", node1Attrs['server.updated_at'])
        // datacenter
        Assert.assertEquals("name_1134955039", node1Attrs['datacenter.name'])
        Assert.assertEquals("description_1627153770", node1Attrs['datacenter.description'])
        // instance
        Assert.assertEquals("attrs" + node1Attrs, "name_1680878585", node1Attrs['instance.name'])
        Assert.assertEquals("resource_3580826380", node1Attrs['instance.resource_uid'])
        Assert.assertEquals("test_publicdns_3435340970.com", node1Attrs['instance.public_dns_name'])
        Assert.assertEquals("test_publicdns_3435340970.com", node1Attrs['instance.public_dns_name'])
        Assert.assertEquals("test_privatedns_2598540039.com", node1Attrs['instance.private_dns_name'])
        Assert.assertEquals("ud_33741804", node1Attrs['instance.user_data'])
        Assert.assertEquals("46.34.17.252", node1Attrs['instance.public_ip_address'])
        Assert.assertEquals("46.34.17.252", node1.getHostname())
        Assert.assertEquals("6.4.5.3", node1Attrs['instance.private_ip_address'])
        Assert.assertEquals("fixed", node1Attrs['instance.pricing_type'])
        // cloud
        Assert.assertEquals("Bob's Eucalyptus 125015575", node1Attrs['cloud.display_name'])
        Assert.assertEquals("eucalyptus", node1Attrs['cloud.cloud_type'])
        // server_template
        Assert.assertEquals("DESCRIPTION_3223207047", node1Attrs['server_template.description'])
        Assert.assertEquals("NICKNAME_2529968303", node1Attrs['server_template.name'])
        Assert.assertEquals("0", node1Attrs['server_template.revision'])
        // image
        Assert.assertEquals("resource_machine_2329921984", node1Attrs['image.resource_uid'])
        Assert.assertEquals("machine", node1Attrs['image.image_type'])

        // input
        Assert.assertEquals("attrs" + node1Attrs, "text:", node1Attrs['inputs.input_definition_3228327932'])

        // ssh_key
        Assert.assertEquals("attrs" + node1Attrs, "resource_913473243", node1Attrs['ssh_key.resource_uid'])
        // tags
        def tags = node1.getTags()
        Assert.assertEquals("tags=" + tags + ".", 3, tags.size())
        Assert.assertTrue("no tag for cloud", tags.contains("Bob's Eucalyptus 125015575"))
        Assert.assertTrue("no tag for datacenter. tags=" + tags, tags.contains("name_1134955039"))
        Assert.assertTrue("no tag for deployment. tags=" + tags, tags.contains("name_3567039744"))
    }

    @Test
    public void getServerArrayNodes() {
        def RightscaleBasicCache cache = new RightscaleBasicCache()
        initializeCache(cache)
        def rightscaleNodes = new RightscaleNodes(createConfigProperties())

        def nodeSet = rightscaleNodes.populateServerArrayNodes(cache)

        Assert.assertEquals(0, nodeSet.getNodes().size())

    }

    @Test
    public void loadCache() {
        /**
         * Create API object to simulate the API service.
         */
        def query = new RightscaleBasicCache()
        initializeCache(query)
        /**
         * Create an empty cache.
         */
        def cache = new RightscaleBasicCache()
        /**
         * Create the nodes object.
         */
        def nodes = new RightscaleNodes(createConfigProperties(), query, cache)
        /**
         * Invoke the loadCache method.
         */
        nodes.loadCache()

        /**
         * Confirm RightscaleNodes has populated the cache with base data.
         */
        Assert.assertEquals(query.getClouds().size(), cache.getClouds().size())
        Assert.assertEquals(query.getDeployments().size(), cache.getDeployments().size())
        Assert.assertEquals(query.getServers().size(), cache.getServers().size())
        Assert.assertEquals(query.getServerTemplates().size(), cache.getServerTemplates().size())
        Assert.assertEquals(query.getServerArrays().size(), cache.getServerArrays().size())

        def cloud = cache.getClouds().get("/api/clouds/926218062")
        Assert.assertEquals("wrong num datacenters", 1, cache.getDatacenters(cloud.getId()).size())
        Assert.assertEquals(1, cache.getInstanceTypes(cloud.getId()).size())
        Assert.assertEquals(1, cache.getSshKeys(cloud.getId()).size())
        Assert.assertEquals("wrong num subnets", 2, cache.getSubnets(cloud.getId()).size())
        Assert.assertEquals(1, cache.getInstances(cloud.getId()).size())
        def instance0 = cache.getInstances(cloud.getId()).values().asList().first()
        Assert.assertTrue(instance0 instanceof InstanceResource)
        // TODO: Check more deeply.
    }



    @Test
    public void getNodes() {
        /**
         * Create API object to simulate the API service.
         */
        def query = new RightscaleBasicCache()
        initializeCache(query)
        /**
         * Create an empty cache.
         */
        def cache = new RightscaleBasicCache()
        /**
         * Create the nodes object.
         */
        def nodes = new RightscaleNodes(createConfigProperties(), query, cache)
        /**
         * Invoke the getNodes method.
         */
        def nodeset = nodes.getNodes()

        Assert.assertEquals(2, nodeset.getNodes().size())
        Assert.assertEquals(false, nodes.needsRefresh())
    }

    @Test

    public void filteredInputs() {

        def query = new RightscaleBasicCache()
        initializeCache(query)

        Properties configuration = createConfigProperties()
        configuration.setProperty(RightscaleNodesFactory.INPUT_PATT, "rs_utils.*")
        def nodes = new RightscaleNodes(configuration, query, new RightscaleBasicCache())

        nodes.loadCache()
        def nodeset = nodes.getNodes()
        nodeset.getNodes().each {
            it.getAttributes().keySet().each {
                if (it.startsWith("inputs.")) {
                    Assert.assertTrue(it.matches("inputs.rs_utils.*"))
                }
            }


        }
    }
}