package com.simplifyops.rundeck.plugin.resources

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class RightscaleCacheTest {

    @Test
    public void constructor() {
        def RightscaleBasicCache cache = new RightscaleBasicCache()
        Assert.assertTrue(cache.getClouds().size()==0)
        Assert.assertTrue(cache.getDatacenters().size()==0)
        Assert.assertTrue(cache.getDeployments().size()==0)
        Assert.assertTrue(cache.getImages().size()==0)
        Assert.assertTrue(cache.getInputs().size()==0)
        Assert.assertTrue(cache.getInstances().size()==0)
        Assert.assertTrue(cache.getInstanceTypes().size()==0)
        Assert.assertTrue(cache.getServerArrays().size()==0)
        Assert.assertTrue(cache.getServerTemplates().size()==0)
        Assert.assertTrue(cache.getServers().size()==0)
        Assert.assertTrue(cache.getSshKeys().size()==0)
        Assert.assertTrue(cache.getTags().size()==0)
    }



    @Test
    public void load() {
        def RightscaleBasicCache cache = new RightscaleBasicCache()
        cache.load {
            def c = new CloudResource()
            c.attributes['name'] = "cloud1"
            cache.updateClouds(["/api/clouds/1": c])
        }
        Assert.assertEquals(1,cache.getClouds().size())
        Assert.assertEquals("cloud1",cache.getClouds().get("/api/clouds/1")['attributes']['name'])
    }

    @Test
    public void updateClouds() {
        def RightscaleBasicCache cache = new RightscaleBasicCache()
         cache.updateClouds(
                CloudResource.burst(new XmlParser().parseText(XmlData.CLOUDS),
                        'cloud', CloudResource.&create))
        def resources = cache.getClouds()
        Assert.assertEquals(2,resources.size())
    }

    @Test
    public void updateDatacenters() {
        def RightscaleBasicCache cache = new RightscaleBasicCache()
        cache.updateDatacenters(
                DatacenterResource.burst(new XmlParser().parseText(XmlData.DATACENTERS),
                        'datacenter', DatacenterResource.&create))
        def resources = cache.getDatacenters()
        Assert.assertEquals(2,resources.size())
    }

    @Test
    public void updateDeployments() {
        def RightscaleBasicCache cache = new RightscaleBasicCache()
        cache.updateDeployments(
                DeploymentResource.burst(new XmlParser().parseText(XmlData.DEPLOYMENTS),
                        'deployment', DatacenterResource.&create))
        def resources = cache.getDeployments()
        Assert.assertEquals(2,resources.size())
    }

    @Test
    public void updateImages() {
        def RightscaleBasicCache cache = new RightscaleBasicCache()
        cache.updateImages(
                ImageResource.burst(new XmlParser().parseText(XmlData.IMAGES),
                        'image', ImageResource.&create))
        def resources = cache.getImages()
        Assert.assertEquals(2,resources.size())
    }
    @Test
    public void getImage() {
        def RightscaleBasicCache cache = new RightscaleBasicCache()
        cache.updateImages(
                ImageResource.burst(new XmlParser().parseText(XmlData.IMAGES),
                        'image', ImageResource.&create))
        def image = cache.getImage("/api/clouds/926218062/images/ABC3344342180DEF")
        Assert.assertEquals("resource_machine_3108560822", image.attributes['resource_uid'])
    }

    @Test
    public void updateInputs() {
        def RightscaleBasicCache cache = new RightscaleBasicCache()
        cache.updateInputs(
                InputResource.burst(new XmlParser().parseText(XmlData.INPUTS),
                        'input', InputResource.&create))
        def resources = cache.getInputs()
        Assert.assertEquals(4,resources.size())
    }

    @Test
    public void updateInstances() {
        def RightscaleBasicCache cache = new RightscaleBasicCache()
        cache.updateInstances(
                InstanceResource.burst(new XmlParser().parseText(XmlData.INSTANCES),
                        'instance', InstanceResource.&create))
        def resources = cache.getInstances()
        Assert.assertEquals(2,resources.size())
    }

    @Test
    public void updateInstanceTypes() {
        def RightscaleBasicCache cache = new RightscaleBasicCache()
        cache.updateInstanceTypes(
                InstanceTypeResource.burst(new XmlParser().parseText(XmlData.INSTANCE_TYPES),
                        'instance_type', InstanceTypeResource.&create))
        def resources = cache.getInstanceTypes()
        Assert.assertEquals(2,resources.size())
    }

    @Test
    public void updateServerArrays() {
        def RightscaleBasicCache cache = new RightscaleBasicCache()
        cache.updateServerArrays(
                ServerArrayResource.burst(new XmlParser().parseText(XmlData.SERVER_ARRAYS),
                        'server_array', ServerArrayResource.&create))
        def resources = cache.getServerArrays()
        Assert.assertEquals(2,resources.size())
    }

    @Test
    public void updateServerTemplates() {
        def RightscaleBasicCache cache = new RightscaleBasicCache()
        cache.updateServerTemplates(
                ServerTemplateResource.burst(new XmlParser().parseText(XmlData.SERVER_TEMPLATES),
                        'server_template', ServerTemplateResource.&create))
        def resources = cache.getServerTemplates()
        Assert.assertEquals(2,resources.size())
    }

    @Test
    public void updateServers() {
        def RightscaleBasicCache cache = new RightscaleBasicCache()
        cache.updateServers(
                ServerResource.burst(new XmlParser().parseText(XmlData.SERVERS),
                        'server', ServerResource.&create))
        def resources = cache.getServers()
        Assert.assertEquals(2,resources.size())
    }

    @Test
    public void updateSshKeys() {
        def RightscaleBasicCache cache = new RightscaleBasicCache()
        cache.updateSshKeys(
                SshKeyResource.burst(new XmlParser().parseText(XmlData.SSH_KEYS),
                        'ssh_key', ServerResource.&create))
        def resources = cache.getSshKeys()
        Assert.assertEquals(2,resources.size())
    }

    /*
    @Test
    public void needsRefresh() {

        def RightscaleBasicCache cache = new RightscaleBasicCache()
        Assert.assertEquals(true,cache.needsRefresh())

        cache.updateServers(
                ServerResource.burst(new XmlParser().parseText(XmlData.SERVERS),
                        'server', ServerResource.&create))
        Assert.assertEquals(false,cache.needsRefresh())

    }
    */
}