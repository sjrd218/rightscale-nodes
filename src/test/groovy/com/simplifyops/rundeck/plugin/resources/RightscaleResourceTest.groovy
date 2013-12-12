package com.simplifyops.rundeck.plugin.resources

import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class RightscaleResourceTest {

    @Test
    public void parse() {
        def xml = new XmlParser().parseText(XmlData.DEPLOYMENTS)
        def r = RightscaleResource.create(xml.children()[0])
        Assert.assertEquals(5, r.links.size())
        Assert.assertEquals("/api/deployments/2/inputs",r.links['inputs'])
        Assert.assertEquals("/api/deployments/2", r.links['self'] )
        Assert.assertEquals("/api/deployments/2/servers", r.links['servers'])
        Assert.assertEquals("/api/deployments/2/server_arrays", r.links['server_arrays'])
        Assert.assertEquals("/api/deployments/2/alerts", r.links['alerts'] )
        Assert.assertEquals("name_1414276446", r.attributes['name'])
    }

    @Test
    public void generateAttributeName() {
        def r = new RightscaleResource()
        Assert.assertEquals("foo", r.generateAttributeName("foo"))
        r.setPrefix('rs')
        Assert.assertEquals("rs.foo", r.generateAttributeName("foo"))
    }

    @Test
    public void populate() {
        def xml = new XmlParser().parseText(XmlData.DEPLOYMENTS)
        def r = RightscaleResource.create(xml.children()[0])
        def newNode = new NodeEntryImpl()
        r.populate(newNode)
        Assert.assertEquals("attributes:"+newNode.getAttributes(), "name_1414276446", newNode.getAttribute("name"))
    }

    @Test
    public void cloud() {
        def xml = new XmlParser().parseText(XmlData.CLOUDS)
        // get the first element from the xml doc.
        def r =  CloudResource.create(xml.children()[0])
        Assert.assertTrue(r instanceof CloudResource)
        Assert.assertNotNull(r)
        Assert.assertEquals(14, r.links.size())
        Assert.assertEquals("Bob's Eucalyptus 2665404372", r.attributes['name'])
        Assert.assertEquals("", r.attributes['description'])
        def newNode = new NodeEntryImpl()
        r.populate(newNode)
        Assert.assertEquals("attributes="+newNode.getAttributes(), "Bob's Eucalyptus 2665404372", newNode.getAttribute("cloud.name"))
        Assert.assertEquals("", newNode.getAttribute("cloud.description"))
        Assert.assertTrue(newNode.getTags().contains('rs:'+r.attributes['name']))

        // burst the XML into a Map of CloudResource objects, keyed by their href
        def Map map = CloudResource.burst(xml, 'cloud', CloudResource.&create)
        Assert.assertEquals(2, map.size())
        def RightscaleResource  cloud1 = map.get("/api/clouds/926218062")
        Assert.assertNotNull(cloud1)
        Assert.assertEquals("/api/clouds/926218062", cloud1.links['self'])
        def RightscaleResource cloud2 = map.get("/api/clouds/929741027")
        Assert.assertNotNull(cloud2)
        Assert.assertEquals("/api/clouds/929741027", cloud2.links['self'])
    }

    @Test
    public void deployment() {
        def xml = new XmlParser().parseText(XmlData.DEPLOYMENTS)
        def r = DeploymentResource.create(xml.children()[0])
        Assert.assertTrue(r instanceof DeploymentResource)
        Assert.assertEquals(5, r.links.size())
        Assert.assertEquals("name_1414276446", r.attributes['name'])
        Assert.assertEquals("description_4175578395", r.attributes['description'])
        def newNode = new NodeEntryImpl()
        r.populate(newNode)
        Assert.assertEquals("name_1414276446", newNode.getAttribute("deployment.name"))
        Assert.assertEquals("description_4175578395", newNode.getAttribute("deployment.description"))
        Assert.assertTrue(newNode.getTags().contains('rs:name_1414276446'))

    }

    @Test
    public void datacenter() {
        def xml = new XmlParser().parseText(XmlData.DATACENTERS)
        def r = DatacenterResource.create(xml.children()[0])
        Assert.assertTrue(r instanceof DatacenterResource)
        Assert.assertEquals(2, r.links.size())
        Assert.assertEquals("name_1124252263", r.attributes['name'])
        Assert.assertEquals("description_3451411839", r.attributes['description'])
        Assert.assertEquals("resource_2381700949", r.attributes['resource_uid'])
        def newNode = new NodeEntryImpl()
        r.populate(newNode)
        Assert.assertEquals("name_1124252263", newNode.getAttribute("datacenter.name"))
        Assert.assertEquals("description_3451411839", newNode.getAttribute("datacenter.description"))
        Assert.assertEquals("resource_2381700949", newNode.getAttribute("datacenter.resource_uid"))
        Assert.assertEquals(1,newNode.getTags().size())
        Assert.assertTrue(newNode.getTags().contains('rs:name_1124252263'))
    }

    @Test
    public void image() {
        def xml = new XmlParser().parseText(XmlData.IMAGES)
        def r = ImageResource.create(xml.children()[0])
        Assert.assertTrue(r instanceof ImageResource)
        Assert.assertEquals(2, r.links.size())
        Assert.assertEquals("name_3395370615", r.attributes['name'])
        Assert.assertEquals("", r.attributes['description'])
        Assert.assertEquals("resource_machine_3108560822", r.attributes['resource_uid'])
        Assert.assertEquals("machine", r.attributes['image_type'])
        Assert.assertEquals("private", r.attributes['visibility'])
        def newNode = new NodeEntryImpl()
        r.populate(newNode)
        Assert.assertEquals("name_3395370615", newNode.getAttribute("image.name"))
        Assert.assertEquals("", newNode.getAttribute("image.description"))
        Assert.assertEquals("resource_machine_3108560822", newNode.getAttribute("image.resource_uid"))
        Assert.assertEquals("machine", newNode.getAttribute("image.image_type"))
        Assert.assertEquals("private", newNode.getAttribute("image.visibility"))
    }

    @Test
    public void instanceType() {
        def xml = new XmlParser().parseText(XmlData.INSTANCE_TYPES)
        def r = InstanceTypeResource.create(xml.children()[0])
        Assert.assertTrue(r instanceof InstanceTypeResource)
        Assert.assertEquals(2, r.links.size())
        Assert.assertEquals("/api/clouds/926218062", r.links['cloud'])
        Assert.assertEquals("name_536147432", r.attributes['name'])
        Assert.assertEquals("description_1832681328", r.attributes['description'])
        Assert.assertEquals("resource_1086586171", r.attributes['resource_uid'])
        Assert.assertEquals("", r.attributes['cpu_architecture'])
        Assert.assertEquals("", r.attributes['cpu_count'])
        Assert.assertEquals("", r.attributes['memory'])
        def newNode = new NodeEntryImpl()
        r.populate(newNode)
        Assert.assertEquals("name_536147432", newNode.getAttribute("instance_type.name"))
        Assert.assertEquals("description_1832681328", newNode.getAttribute("instance_type.description"))
        Assert.assertEquals("resource_1086586171", newNode.getAttribute("instance_type.resource_uid"))
        Assert.assertEquals("", newNode.getAttribute("instance_type.cpu_architecture"))
        Assert.assertEquals("", newNode.getAttribute("instance_type.cpu_count"))
        Assert.assertEquals("", newNode.getAttribute("instance_type.memory"))
    }

    @Test
    public void instance() {
        def xml = new XmlParser().parseText(XmlData.INSTANCES)
        def r = InstanceResource.create(xml.children()[0])
        Assert.assertTrue(r instanceof InstanceResource)
        Assert.assertEquals(16, r.links.size())
        Assert.assertEquals("/api/clouds/926218062", r.links['cloud'])
        Assert.assertEquals("/api/deployments/2", r.links['deployment'])
        Assert.assertEquals("/api/clouds/926218062/datacenters/ABC3119440049DEF", r.links['datacenter'])
        Assert.assertEquals("/api/clouds/926218062/ssh_keys/ABC3518369342DEF", r.links['ssh_key'])
        Assert.assertEquals("name_642936808", r.attributes['name'])
        Assert.assertEquals("", r.attributes['description'])
        Assert.assertEquals("resource_369673713", r.attributes['resource_uid'])
        Assert.assertEquals("81.31.222.93", r.attributes['public_ip_address'])
        Assert.assertEquals("2.8.5.5", r.attributes['private_ip_address'])
        Assert.assertEquals("test_publicdns_3235168145.com", r.attributes['public_dns_name'])
        Assert.assertEquals("test_privatedns_1838680768.com", r.attributes['private_dns_name'])
        Assert.assertEquals("operational", r.attributes['state'])
        Assert.assertEquals("2013/10/02 01:38:47 +0000", r.attributes['updated_at'])
        def newNode = new NodeEntryImpl()
        r.populate(newNode)
        Assert.assertEquals("name_642936808", newNode.getAttribute("instance.name"))
        Assert.assertEquals("", newNode.getAttribute("instance.description"))
        Assert.assertEquals("resource_369673713", newNode.getAttribute("instance.resource_uid"))
        Assert.assertEquals("81.31.222.93", newNode.getAttribute("instance.public_ip_address"))
        Assert.assertEquals("2.8.5.5", newNode.getAttribute("instance.private_ip_address"))
        Assert.assertEquals("test_publicdns_3235168145.com", newNode.getAttribute("instance.public_dns_name"))
        Assert.assertEquals("test_privatedns_1838680768.com", newNode.getAttribute("instance.private_dns_name"))
        Assert.assertEquals("operational", newNode.getAttribute("instance.state"))
        Assert.assertEquals("2013/10/02 01:38:47 +0000", newNode.getAttribute("instance.updated_at"))
    }

    @Test
    public void server() {
        def xml = new XmlParser().parseText(XmlData.SERVERS)
        def r = ServerResource.create(xml.children()[0])
        Assert.assertTrue(r instanceof ServerResource)
        Assert.assertEquals(7, r.links.size())
        Assert.assertTrue(r.links.containsKey('current_instance'))
        Assert.assertTrue(r.links.containsKey('cloud')) // this is a link generated during the burst.
        Assert.assertEquals("/api/clouds/926218062", r.links['cloud']) // this is a link generated during the burst.
        Assert.assertEquals("name_3094109966", r.attributes['name'])
        Assert.assertEquals("operational", r.attributes['state'] )
        Assert.assertEquals("2013/10/02 01:38:46 +0000", r.attributes['created_at'])
        Assert.assertEquals("2013/10/02 01:38:47 +0000", r.attributes['updated_at'])
        def newNode = new NodeEntryImpl()
        r.populate(newNode)
        Assert.assertEquals("name_3094109966", newNode.getAttribute("server.name"))
        Assert.assertEquals("operational", newNode.getAttribute("server.state"))
        Assert.assertEquals("2013/10/02 01:38:46 +0000", newNode.getAttribute("server.created_at"))
        Assert.assertEquals("2013/10/02 01:38:47 +0000", newNode.getAttribute("server.updated_at"))
    }

    @Test
    public void serverArray() {
        def xml = new XmlParser().parseText(XmlData.SERVER_ARRAYS)
        def r = ServerArrayResource.create(xml.children()[0])
        Assert.assertTrue(r instanceof ServerArrayResource)
        Assert.assertEquals(6, r.links.size())
        Assert.assertEquals("name_767376035", r.attributes['name'])
        Assert.assertEquals("2", r.attributes['min_count'])
        Assert.assertEquals("3", r.attributes['max_count'])
        Assert.assertEquals("alert", r.attributes['array_type'])
        Assert.assertEquals("enabled", r.attributes['state'])
        Assert.assertEquals("0", r.attributes['instances_count'])
        def newNode = new NodeEntryImpl()
        r.populate(newNode)
        Assert.assertEquals("name_767376035", newNode.getAttribute("server_array.name"))
        Assert.assertEquals("2", newNode.getAttribute("server_array.min_count"))
        Assert.assertEquals("3", newNode.getAttribute("server_array.max_count"))
        Assert.assertEquals("alert", newNode.getAttribute("server_array.array_type"))
        Assert.assertEquals("enabled", newNode.getAttribute("server_array.state"))
        Assert.assertEquals("0", newNode.getAttribute("server_array.instances_count"))
        Assert.assertEquals(1,newNode.getTags().size())
        Assert.assertTrue(newNode.getTags().contains('rs:array=name_767376035'))
    }

    @Test
    public void serverTemplate() {
        def xml = new XmlParser().parseText(XmlData.SERVER_TEMPLATES)
        def r = ServerTemplateResource.create(xml.children()[0])
        Assert.assertTrue(r instanceof ServerTemplateResource)
        Assert.assertEquals(7, r.links.size())
        Assert.assertEquals("NICKNAME_1948317086", r.attributes['name'])
        Assert.assertEquals("DESCRIPTION_3633115567", r.attributes['description'])
        Assert.assertEquals("0", r.attributes['revision'])
        def newNode = new NodeEntryImpl()
        r.populate(newNode)
        Assert.assertEquals("NICKNAME_1948317086", newNode.getAttribute("server_template.name"))
        Assert.assertEquals("DESCRIPTION_3633115567", newNode.getAttribute("server_template.description"))
        Assert.assertEquals("0", newNode.getAttribute("server_template.revision"))
    }

    @Test
    public void subnet() {
        def xml = new XmlParser().parseText(XmlData.SUBNETS)
        def r = SubnetResource.create(xml.children()[0])
        Assert.assertTrue(r instanceof SubnetResource)
        Assert.assertEquals(2, r.links.size())
        Assert.assertEquals("some_name", r.attributes['name'])
        Assert.assertEquals("subnet2729453100", r.attributes['description'])
        Assert.assertEquals("public", r.attributes['visibility'])
        Assert.assertEquals("pending", r.attributes['state'])
        Assert.assertEquals("i-920789752", r.attributes['resource_uid'])
        Assert.assertEquals("", r.attributes['cidr_block'])
        def newNode = new NodeEntryImpl()
        r.populate(newNode)
        Assert.assertEquals("some_name", newNode.getAttribute("subnet.name"))
        Assert.assertEquals("subnet2729453100", newNode.getAttribute("subnet.description"))
        Assert.assertEquals("public", newNode.getAttribute("subnet.visibility"))
        Assert.assertEquals("pending", newNode.getAttribute("subnet.state"))
        Assert.assertEquals("i-920789752", newNode.getAttribute("subnet.resource_uid"))
        Assert.assertEquals("", newNode.getAttribute("subnet.cidr_block"))
    }

    @Test
    public void sshkey() {
        def xml = new XmlParser().parseText(XmlData.SSH_KEYS)
        def r = SshKeyResource.create(xml.children()[0])
        Assert.assertTrue(r instanceof SshKeyResource)
        Assert.assertEquals(2, r.links.size())
        Assert.assertEquals("resource_4149218284", r.attributes['resource_uid'])

        def newNode = new NodeEntryImpl()
        r.populate(newNode)
        Assert.assertEquals("resource_4149218284", newNode.getAttribute("ssh_key.resource_uid"))
    }

    @Test
    public void tags() {
        def xml = new XmlParser().parseText(XmlData.TAGS)
        def r =  TagsResource.create(xml.children()[0])
        Assert.assertTrue(r instanceof TagsResource)
        Assert.assertEquals(1, r.links.size())

        def Collection tags = ((String)r.attributes['tags']).split(",")
        Assert.assertEquals(2, tags.size())

        Assert.assertTrue("tag not found, color:red=false.", tags.contains("color:red=false"))
        Assert.assertTrue("tag not found, color:blue=true.", tags.contains("color:blue=true"))

        def newNode = new NodeEntryImpl()
        r.populate(newNode)
        Assert.assertEquals(2,newNode.getTags().size())
        Assert.assertTrue(newNode.getTags().contains('rs:color:red=false'))
    }
}
