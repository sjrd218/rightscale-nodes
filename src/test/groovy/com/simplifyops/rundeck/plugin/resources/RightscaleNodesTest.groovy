package com.simplifyops.rundeck.plugin.resources

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
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
    void initializeCache(RightscaleCache cache) {
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
                InputsResource.burst(new XmlParser().parseText(XmlData.INPUTS),
                        'input', InputsResource.&create))
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
    }


    @Test
    public void cacheRefresh() {
        def RightscaleCache cache = new RightscaleCache()
        initializeCache(cache)
        cache.cleanUp()
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
        return configuration
    }

    @Test
    public void createNode() {
        RightscaleNodes rightscaleNodes = new RightscaleNodes(createConfigProperties())
        def node = rightscaleNodes.createNode('node1')
        Assert.assertEquals("unix",node.getOsFamily())
        Assert.assertEquals("admin",node.getUsername())
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
    public void queryServers() {
        def RightscaleCache cache = new RightscaleCache()
        initializeCache(cache)
        def rightscaleNodes = new RightscaleNodes(createConfigProperties())

        def nodeSet =  rightscaleNodes.queryServers(cache)

        Assert.assertEquals(2,nodeSet.getNodes().size())
        def nodenames = nodeSet.getNodeNames()
        Assert.assertTrue("name_2149427003 not found. nodes:"+nodenames, nodenames.contains("name_2149427003"))
        Assert.assertTrue("name_3094109966 not found. nodes:"+nodenames, nodenames.contains("name_3094109966"))
        def INodeEntry node1 = nodeSet.getNodes().toArray()[0]
        def node1Attrs = node1.getAttributes()
        // server
        Assert.assertEquals("attrs"+node1Attrs, "operational",node1Attrs['server.state'])
        Assert.assertEquals("name_2149427003",node1Attrs['server.name'])
        Assert.assertEquals("description_3746459714",node1Attrs['server.description'])
        Assert.assertEquals("2013/10/02 01:38:50 +0000",node1Attrs['server.created_at'])
        Assert.assertEquals("2013/10/02 01:38:50 +0000",node1Attrs['server.updated_at'])
        // datacenter
        Assert.assertEquals("name_3567039744",node1Attrs['datacenter.name'])
        Assert.assertEquals("description_4289106727",node1Attrs['datacenter.description'])
        // instance
        Assert.assertEquals("attrs"+node1Attrs,"name_1680878585",node1Attrs['instance.name'])
        Assert.assertEquals("resource_3580826380",node1Attrs['instance.resource_uid'])
        Assert.assertEquals("test_publicdns_3435340970.com",node1Attrs['instance.public_dns_name'])
        Assert.assertEquals("test_publicdns_3435340970.com",node1Attrs['instance.public_dns_name'])
        Assert.assertEquals("test_privatedns_2598540039.com",node1Attrs['instance.private_dns_name'])
        Assert.assertEquals("ud_33741804",node1Attrs['instance.user_data'])
        Assert.assertEquals("46.34.17.252",node1Attrs['instance.public_ip_address'])
        Assert.assertEquals("6.4.5.3",node1Attrs['instance.private_ip_address'])
        Assert.assertEquals("fixed",node1Attrs['instance.pricing_type'])
        // cloud
        Assert.assertEquals("Bob's Eucalyptus 125015575",node1Attrs['cloud.display_name'])
        Assert.assertEquals("eucalyptus",node1Attrs['cloud.cloud_type'])
        // server_template
        Assert.assertEquals("DESCRIPTION_3223207047",node1Attrs['server_template.description'])
        Assert.assertEquals("NICKNAME_2529968303",node1Attrs['server_template.name'])
        Assert.assertEquals("0",node1Attrs['server_template.revision'])
        // image
        Assert.assertEquals("resource_machine_2329921984",node1Attrs['image.resource_uid'])
        Assert.assertEquals("machine",node1Attrs['image.image_type'])
        // ssh_key
        //Assert.assertEquals("attrs"+node1Attrs,"resource_4149218284",node1Attrs['ssh_key.resource_uid'])

        // tags
        Assert.assertEquals("tags:"+node1.getTags(),"rs:name_3567039744,rs:Bob's Eucalyptus 125015575",node1.getTags().join(","))
    }
    @Test
    public void queryServerArrays() {
        def RightscaleCache cache = new RightscaleCache()
        initializeCache(cache)
        def rightscaleNodes = new RightscaleNodes(createConfigProperties())

        def nodeSet =  rightscaleNodes.queryServerArrays(cache)

        Assert.assertEquals(0,nodeSet.getNodes().size())

    }
}