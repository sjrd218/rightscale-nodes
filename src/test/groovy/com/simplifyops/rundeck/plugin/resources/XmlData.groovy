package com.simplifyops.rundeck.plugin.resources


public class XmlData {
static public def CLOUDS = '''
    <clouds>
      <cloud>
        <links>
          <link rel="self" href="/api/clouds/926218062"/>
          <link rel="datacenters" href="/api/clouds/926218062/datacenters"/>
          <link rel="instance_types" href="/api/clouds/926218062/instance_types"/>
          <link rel="security_groups" href="/api/clouds/926218062/security_groups"/>
          <link rel="instances" href="/api/clouds/926218062/instances"/>
          <link rel="ssh_keys" href="/api/clouds/926218062/ssh_keys"/>
          <link rel="images" href="/api/clouds/926218062/images"/>
          <link rel="ip_addresses" href="/api/clouds/926218062/ip_addresses"/>
          <link rel="ip_address_bindings" href="/api/clouds/926218062/ip_address_bindings"/>
          <link rel="volume_attachments" href="/api/clouds/926218062/volume_attachments"/>
          <link rel="recurring_volume_attachments" href="/api/clouds/926218062/recurring_volume_attachments"/>
          <link rel="volume_snapshots" href="/api/clouds/926218062/volume_snapshots"/>
          <link rel="volume_types" href="/api/clouds/926218062/volume_types"/>
          <link rel="volumes" href="/api/clouds/926218062/volumes"/>
        </links>
        <description></description>
        <cloud_type>eucalyptus</cloud_type>
        <name>Bob's Eucalyptus 2665404372</name>
        <display_name>Bob's Eucalyptus 2665404372</display_name>
      </cloud>
      <cloud>
        <links>
          <link rel="self" href="/api/clouds/929741027"/>
          <link rel="datacenters" href="/api/clouds/929741027/datacenters"/>
          <link rel="instance_types" href="/api/clouds/929741027/instance_types"/>
          <link rel="security_groups" href="/api/clouds/929741027/security_groups"/>
          <link rel="instances" href="/api/clouds/929741027/instances"/>
          <link rel="ssh_keys" href="/api/clouds/929741027/ssh_keys"/>
          <link rel="images" href="/api/clouds/929741027/images"/>
          <link rel="ip_addresses" href="/api/clouds/929741027/ip_addresses"/>
          <link rel="ip_address_bindings" href="/api/clouds/929741027/ip_address_bindings"/>
          <link rel="volume_attachments" href="/api/clouds/929741027/volume_attachments"/>
          <link rel="recurring_volume_attachments" href="/api/clouds/929741027/recurring_volume_attachments"/>
          <link rel="volume_snapshots" href="/api/clouds/929741027/volume_snapshots"/>
          <link rel="volume_types" href="/api/clouds/929741027/volume_types"/>
          <link rel="volumes" href="/api/clouds/929741027/volumes"/>
        </links>
        <description></description>
        <cloud_type>eucalyptus</cloud_type>
        <name>Bob's Eucalyptus 125015575</name>
        <display_name>Bob's Eucalyptus 125015575</display_name>
      </cloud>
    </clouds>
  '''
static def DATACENTERS = '''
  <datacenters>
      <datacenter>
        <links>
          <link rel="self" href="/api/clouds/926218062/datacenters/ABC1721297747DEF"/>
          <link rel="cloud" href="/api/clouds/926218062"/>
        </links>
        <description>description_3451411839</description>
        <actions></actions>
        <resource_uid>resource_2381700949</resource_uid>
        <name>name_1124252263</name>
      </datacenter>
      <datacenter>
        <links>
          <link rel="self" href="/api/clouds/929741027/datacenters/ABC3187271458DEF"/>
          <link rel="cloud" href="/api/clouds/929741027"/>
        </links>
        <description>description_1627153770</description>
        <actions></actions>
        <resource_uid>resource_527381494</resource_uid>
        <name>name_1134955039</name>
      </datacenter>
      <datacenter>
        <links>
          <link rel="self" href="/api/clouds/926218062/datacenters/ABC3119440049DEF"/>
          <link rel="cloud" href="/api/clouds/929741027"/>
        </links>
        <description>description_1627153770</description>
        <actions></actions>
        <resource_uid>resource_527381494</resource_uid>
        <name>name_1134955039</name>
      </datacenter>

    </datacenters>
    '''
static def DEPLOYMENTS = '''
    <deployments>
      <deployment>
        <links>
          <link rel="inputs" href="/api/deployments/2/inputs"/>
          <link rel="self" href="/api/deployments/2"/>
          <link rel="servers" href="/api/deployments/2/servers"/>
          <link rel="server_arrays" href="/api/deployments/2/server_arrays"/>
          <link rel="alerts" href="/api/deployments/2/alerts"/>
        </links>
        <locked>false</locked>
        <description>description_4175578395</description>
        <actions>
          <action rel="clone"/>
          <action rel="lock"/>
          <action rel="unlock"/>
        </actions>
        <server_tag_scope>deployment</server_tag_scope>
        <name>name_1414276446</name>
      </deployment>
      <deployment>
        <links>
          <link rel="inputs" href="/api/deployments/9/inputs"/>
          <link rel="self" href="/api/deployments/9"/>
          <link rel="servers" href="/api/deployments/9/servers"/>
          <link rel="server_arrays" href="/api/deployments/9/server_arrays"/>
          <link rel="alerts" href="/api/deployments/9/alerts"/>
        </links>
        <locked>false</locked>
        <description>description_4289106727</description>
        <actions>
          <action rel="clone"/>
          <action rel="lock"/>
          <action rel="unlock"/>
        </actions>
        <server_tag_scope>deployment</server_tag_scope>
        <name>name_3567039744</name>
      </deployment>
    </deployments>
      '''
static def IMAGES = '''
    <images>
      <image>
        <links>
          <link rel="self" href="/api/clouds/926218062/images/ABC3344342180DEF"/>
          <link rel="cloud" href="/api/clouds/926218062"/>
        </links>
        <virtualization_type></virtualization_type>
        <cpu_architecture></cpu_architecture>
        <image_type>machine</image_type>
        <description></description>
        <visibility>private</visibility>
        <actions></actions>
        <os_platform></os_platform>
        <resource_uid>resource_machine_3108560822</resource_uid>
        <name>name_3395370615</name>
      </image>
      <image>
        <links>
          <link rel="self" href="/api/clouds/929741027/images/ABC2795121072DEF"/>
          <link rel="cloud" href="/api/clouds/929741027"/>
        </links>
        <virtualization_type></virtualization_type>
        <cpu_architecture></cpu_architecture>
        <image_type>machine</image_type>
        <description></description>
        <visibility>private</visibility>
        <actions></actions>
        <os_platform></os_platform>
        <resource_uid>resource_machine_2329921984</resource_uid>
        <name>name_160576049</name>
      </image>
    </images>
    '''
    static def INPUTS = '''
    <inputs>
      <input>
        <value>text:</value>
        <name>input_definition_3228327932</name>
      </input>
      <input>
        <value>text:</value>
        <name>input_definition_1373183366</name>
      </input>
      <input>
        <value>text:ssh/ssh* cron/cron*</value>
        <name>rs_utils/process_match_list</name>
      </input>
      <input>
        <value>ignore:$ignore</value>
        <name>rs_utils/private_ssh_key</name>
      </input>
    </inputs>
    '''
static def INSTANCE_TYPES ='''
   <instance_types>
     <instance_type>
        <links>
          <link rel="self" href="/api/clouds/926218062/instance_types/ABC2961793435DEF"/>
          <link rel="cloud" href="/api/clouds/926218062"/>
        </links>
        <cpu_architecture></cpu_architecture>
        <description>description_1832681328</description>
        <actions></actions>
        <resource_uid>resource_1086586171</resource_uid>
        <local_disks></local_disks>
        <cpu_speed></cpu_speed>
        <cpu_count></cpu_count>
        <name>name_536147432</name>
        <local_disk_size></local_disk_size>
        <memory></memory>
      </instance_type>
      <instance_type>
        <links>
          <link rel="self" href="/api/clouds/929741027/instance_types/ABC885107623DEF"/>
          <link rel="cloud" href="/api/clouds/929741027"/>
        </links>
        <cpu_architecture></cpu_architecture>
        <description>description_2129855735</description>
        <actions></actions>
        <resource_uid>resource_2335400643</resource_uid>
        <local_disks></local_disks>
        <cpu_speed></cpu_speed>
        <cpu_count></cpu_count>
        <name>name_3816596994</name>
        <local_disk_size></local_disk_size>
        <memory></memory>
      </instance_type>
    </instance_types>
    '''
static def INSTANCES = '''
    <instances>
      <instance>
        <locked>false</locked>
        <links>
          <link rel="self" href="/api/clouds/926218062/instances/ABC2004049895DEF"/>
          <link rel="cloud" href="/api/clouds/926218062"/>
          <link rel="deployment" href="/api/deployments/2"/>
          <link rel="server_template" href="/api/server_templates/1"/>
          <link rel="multi_cloud_image" href="/api/multi_cloud_images/2" inherited_source="server_template"/>
          <link rel="parent" href="/api/servers/1"/>
          <link rel="volume_attachments" href="/api/clouds/926218062/instances/ABC2004049895DEF/volume_attachments"/>
          <link rel="inputs" href="/api/clouds/926218062/instances/ABC2004049895DEF/inputs"/>
          <link rel="monitoring_metrics" href="/api/clouds/926218062/instances/ABC2004049895DEF/monitoring_metrics"/>
          <link rel="alerts" href="/api/clouds/926218062/instances/ABC2004049895DEF/alerts"/>
          <link rel="image" href="/api/clouds/926218062/images/ABC3344342180DEF"/>
          <link rel="ramdisk_image" href="/api/clouds/926218062/images/ABC3134862298DEF"/>
          <link rel="kernel_image" href="/api/clouds/926218062/images/ABC228146662DEF"/>
          <link rel="instance_type" href="/api/clouds/926218062/instance_types/ABC2961793435DEF"/>
          <link rel="ssh_key" href="/api/clouds/926218062/ssh_keys/ABC3518369342DEF"/>
          <link rel="datacenter" href="/api/clouds/926218062/datacenters/ABC3119440049DEF"/>
        </links>
        <description></description>
        <terminated_at></terminated_at>
        <public_ip_addresses>
          <public_ip_address>81.31.222.93</public_ip_address>
        </public_ip_addresses>
        <monitoring_id>monitoring2297385397</monitoring_id>
        <created_at>2013/10/02 01:38:47 +0000</created_at>
        <private_ip_addresses>
          <private_ip_address>2.8.5.5</private_ip_address>
        </private_ip_addresses>
        <actions>
          <action rel="terminate"/>
          <action rel="reboot"/>
          <action rel="run_executable"/>
          <action rel="lock"/>
          <action rel="unlock"/>
        </actions>
        <os_platform></os_platform>
        <resource_uid>resource_369673713</resource_uid>
        <monitoring_server>sketchy2269998029</monitoring_server>
        <public_dns_names>
          <public_dns_name>test_publicdns_3235168145.com</public_dns_name>
        </public_dns_names>
        <security_groups></security_groups>
        <user_data>ud_737916590</user_data>
        <private_dns_names>
          <private_dns_name>test_privatedns_1838680768.com</private_dns_name>
        </private_dns_names>
        <pricing_type>fixed</pricing_type>
        <name>name_642936808</name>
        <subnets></subnets>
        <state>operational</state>
        <updated_at>2013/10/02 01:38:47 +0000</updated_at>
      </instance>
      <instance>
        <locked>false</locked>
        <links>
          <link rel="self" href="/api/clouds/929741027/instances/ABC3511502537DEF"/>
          <link rel="cloud" href="/api/clouds/929741027"/>
          <link rel="deployment" href="/api/deployments/9"/>
          <link rel="server_template" href="/api/server_templates/5"/>
          <link rel="multi_cloud_image" href="/api/multi_cloud_images/4" inherited_source="server_template"/>
          <link rel="parent" href="/api/servers/3"/>
          <link rel="volume_attachments" href="/api/clouds/929741027/instances/ABC3511502537DEF/volume_attachments"/>
          <link rel="inputs" href="/api/clouds/929741027/instances/ABC3511502537DEF/inputs"/>
          <link rel="monitoring_metrics" href="/api/clouds/929741027/instances/ABC3511502537DEF/monitoring_metrics"/>
          <link rel="alerts" href="/api/clouds/929741027/instances/ABC3511502537DEF/alerts"/>
          <link rel="image" href="/api/clouds/929741027/images/ABC2795121072DEF"/>
          <link rel="ramdisk_image" href="/api/clouds/929741027/images/ABC74889710DEF"/>
          <link rel="kernel_image" href="/api/clouds/929741027/images/ABC1729329719DEF"/>
          <link rel="instance_type" href="/api/clouds/929741027/instance_types/ABC885107623DEF"/>
          <link rel="ssh_key" href="/api/clouds/929741027/ssh_keys/ABC2568952498DEF"/>
          <link rel="datacenter" href="/api/clouds/926218062/datacenters/ABC3119440049DEF"/>
        </links>
        <description></description>
        <terminated_at></terminated_at>
        <public_ip_addresses>
          <public_ip_address>46.34.17.252</public_ip_address>
        </public_ip_addresses>
        <monitoring_id>monitoring2909813380</monitoring_id>
        <created_at>2013/10/02 01:38:50 +0000</created_at>
        <private_ip_addresses>
          <private_ip_address>6.4.5.3</private_ip_address>
        </private_ip_addresses>
        <actions>
          <action rel="terminate"/>
          <action rel="reboot"/>
          <action rel="run_executable"/>
          <action rel="lock"/>
          <action rel="unlock"/>
        </actions>
        <os_platform></os_platform>
        <resource_uid>resource_3580826380</resource_uid>
        <monitoring_server>sketchy3910831462</monitoring_server>
        <public_dns_names>
          <public_dns_name>test_publicdns_3435340970.com</public_dns_name>
        </public_dns_names>
        <security_groups></security_groups>
        <user_data>ud_33741804</user_data>
        <private_dns_names>
          <private_dns_name>test_privatedns_2598540039.com</private_dns_name>
        </private_dns_names>
        <pricing_type>fixed</pricing_type>
        <name>name_1680878585</name>
        <subnets></subnets>
        <state>operational</state>
        <updated_at>2013/10/02 01:38:50 +0000</updated_at>
      </instance>
    </instances>
    '''
static def SERVERS = '''
<servers>
  <server>
    <links>
      <link rel="self" href="/api/servers/1"/>
      <link rel="deployment" href="/api/deployments/2"/>
      <link rel="current_instance" href="/api/clouds/926218062/instances/ABC2004049895DEF"/>
      <link rel="next_instance" href="/api/clouds/926218062/instances/C3NJJDBV6DHPF"/>
      <link rel="alert_specs" href="/api/servers/1/alert_specs"/>
      <link rel="alerts" href="/api/servers/1/alerts"/>
    </links>
    <current_instance>
      <locked>false</locked>
      <links>
        <link rel="self" href="/api/clouds/926218062/instances/ABC2004049895DEF"/>
        <link rel="cloud" href="/api/clouds/926218062"/>
        <link rel="deployment" href="/api/deployments/2"/>
        <link rel="server_template" href="/api/server_templates/1"/>
        <link rel="multi_cloud_image" href="/api/multi_cloud_images/2" inherited_source="server_template"/>
        <link rel="parent" href="/api/servers/1"/>
        <link rel="volume_attachments" href="/api/clouds/926218062/instances/ABC2004049895DEF/volume_attachments"/>
        <link rel="inputs" href="/api/clouds/926218062/instances/ABC2004049895DEF/inputs"/>
        <link rel="monitoring_metrics" href="/api/clouds/926218062/instances/ABC2004049895DEF/monitoring_metrics"/>
        <link rel="alerts" href="/api/clouds/926218062/instances/ABC2004049895DEF/alerts"/>
      </links>
      <public_ip_addresses>
        <public_ip_address>81.31.222.93</public_ip_address>
      </public_ip_addresses>
      <created_at>2013/10/02 01:38:47 +0000</created_at>
      <private_ip_addresses>
        <private_ip_address>2.8.5.5</private_ip_address>
      </private_ip_addresses>
      <actions>
        <action rel="terminate"/>
        <action rel="reboot"/>
        <action rel="run_executable"/>
        <action rel="lock"/>
        <action rel="unlock"/>
      </actions>
      <resource_uid>resource_369673713</resource_uid>
      <pricing_type>fixed</pricing_type>
      <name>name_642936808</name>
      <state>operational</state>
      <updated_at>2013/10/02 01:38:47 +0000</updated_at>
    </current_instance>
    <description>description_4161312866</description>
    <created_at>2013/10/02 01:38:46 +0000</created_at>
    <actions>
      <action rel="terminate"/>
      <action rel="clone"/>
    </actions>
    <name>name_3094109966</name>
    <next_instance>
      <locked>false</locked>
      <links>
        <link rel="self" href="/api/clouds/926218062/instances/C3NJJDBV6DHPF"/>
        <link rel="cloud" href="/api/clouds/926218062"/>
        <link rel="deployment" href="/api/deployments/2"/>
        <link rel="server_template" href="/api/server_templates/1"/>
        <link rel="multi_cloud_image" href="/api/multi_cloud_images/2" inherited_source="server_template"/>
        <link rel="parent" href="/api/servers/1"/>
        <link rel="volume_attachments" href="/api/clouds/926218062/instances/C3NJJDBV6DHPF/volume_attachments"/>
        <link rel="inputs" href="/api/clouds/926218062/instances/C3NJJDBV6DHPF/inputs"/>
        <link rel="monitoring_metrics" href="/api/clouds/926218062/instances/C3NJJDBV6DHPF/monitoring_metrics"/>
        <link rel="alerts" href="/api/clouds/926218062/instances/C3NJJDBV6DHPF/alerts"/>
      </links>
      <public_ip_addresses></public_ip_addresses>
      <created_at>2013/10/02 01:38:46 +0000</created_at>
      <private_ip_addresses></private_ip_addresses>
      <actions></actions>
      <resource_uid>61484530-2b03-11e3-9b39-6adbd225978a</resource_uid>
      <pricing_type>fixed</pricing_type>
      <name>name_3094109966</name>
      <state>inactive</state>
      <updated_at>2013/10/02 01:38:46 +0000</updated_at>
    </next_instance>
    <state>operational</state>
    <updated_at>2013/10/02 01:38:47 +0000</updated_at>
  </server>
  <server>
    <links>
      <link rel="self" href="/api/servers/3"/>
      <link rel="deployment" href="/api/deployments/9"/>
      <link rel="current_instance" href="/api/clouds/929741027/instances/ABC3511502537DEF"/>
      <link rel="next_instance" href="/api/clouds/929741027/instances/C4QPE5IR85AQS"/>
      <link rel="alert_specs" href="/api/servers/3/alert_specs"/>
      <link rel="alerts" href="/api/servers/3/alerts"/>
    </links>
    <current_instance>
      <locked>false</locked>
      <links>
        <link rel="self" href="/api/clouds/929741027/instances/ABC3511502537DEF"/>
        <link rel="cloud" href="/api/clouds/929741027"/>
        <link rel="deployment" href="/api/deployments/9"/>
        <link rel="server_template" href="/api/server_templates/5"/>
        <link rel="multi_cloud_image" href="/api/multi_cloud_images/4" inherited_source="server_template"/>
        <link rel="parent" href="/api/servers/3"/>
        <link rel="volume_attachments" href="/api/clouds/929741027/instances/ABC3511502537DEF/volume_attachments"/>
        <link rel="inputs" href="/api/clouds/929741027/instances/ABC3511502537DEF/inputs"/>
        <link rel="monitoring_metrics" href="/api/clouds/929741027/instances/ABC3511502537DEF/monitoring_metrics"/>
        <link rel="alerts" href="/api/clouds/929741027/instances/ABC3511502537DEF/alerts"/>
      </links>
      <public_ip_addresses>
        <public_ip_address>46.34.17.252</public_ip_address>
      </public_ip_addresses>
      <created_at>2013/10/02 01:38:50 +0000</created_at>
      <private_ip_addresses>
        <private_ip_address>6.4.5.3</private_ip_address>
      </private_ip_addresses>
      <actions>
        <action rel="terminate"/>
        <action rel="reboot"/>
        <action rel="run_executable"/>
        <action rel="lock"/>
        <action rel="unlock"/>
      </actions>
      <resource_uid>resource_3580826380</resource_uid>
      <pricing_type>fixed</pricing_type>
      <name>name_1680878585</name>
      <state>operational</state>
      <updated_at>2013/10/02 01:38:50 +0000</updated_at>
    </current_instance>
    <description>description_3746459714</description>
    <created_at>2013/10/02 01:38:50 +0000</created_at>
    <actions>
      <action rel="terminate"/>
      <action rel="clone"/>
    </actions>
    <name>name_2149427003</name>
    <next_instance>
      <locked>false</locked>
      <links>
        <link rel="self" href="/api/clouds/929741027/instances/C4QPE5IR85AQS"/>
        <link rel="cloud" href="/api/clouds/929741027"/>
        <link rel="deployment" href="/api/deployments/9"/>
        <link rel="server_template" href="/api/server_templates/5"/>
        <link rel="multi_cloud_image" href="/api/multi_cloud_images/4" inherited_source="server_template"/>
        <link rel="parent" href="/api/servers/3"/>
        <link rel="volume_attachments" href="/api/clouds/929741027/instances/C4QPE5IR85AQS/volume_attachments"/>
        <link rel="inputs" href="/api/clouds/929741027/instances/C4QPE5IR85AQS/inputs"/>
        <link rel="monitoring_metrics" href="/api/clouds/929741027/instances/C4QPE5IR85AQS/monitoring_metrics"/>
        <link rel="alerts" href="/api/clouds/929741027/instances/C4QPE5IR85AQS/alerts"/>
      </links>
      <public_ip_addresses></public_ip_addresses>
      <created_at>2013/10/02 01:38:50 +0000</created_at>
      <private_ip_addresses></private_ip_addresses>
      <actions></actions>
      <resource_uid>636887a8-2b03-11e3-9b39-6adbd225978a</resource_uid>
      <pricing_type>fixed</pricing_type>
      <name>name_2149427003</name>
      <state>inactive</state>
      <updated_at>2013/10/02 01:38:50 +0000</updated_at>
    </next_instance>
    <state>operational</state>
    <updated_at>2013/10/02 01:38:50 +0000</updated_at>
  </server>
</servers>
  '''
static def SERVER_ARRAYS = '''
    <server_arrays>
      <server_array>
        <links>
          <link rel="self" href="/api/server_arrays/1"/>
          <link rel="deployment" href="/api/deployments/3"/>
          <link rel="current_instances" href="/api/server_arrays/1/current_instances"/>
          <link rel="next_instance" href="/api/clouds/926218062/instances/AMCD4DJMO9IA6"/>
          <link rel="alert_specs" href="/api/server_arrays/1/alert_specs"/>
          <link rel="alerts" href="/api/server_arrays/1/alerts"/>
        </links>
        <description>description_4126386062</description>
        <actions>
          <action rel="clone"/>
        </actions>
        <array_type>alert</array_type>
        <elasticity_params>
          <bounds>
            <min_count>2</min_count>
            <max_count>3</max_count>
          </bounds>
          <pacing>
            <resize_up_by>10</resize_up_by>
            <resize_down_by>5</resize_down_by>
            <resize_calm_time>6</resize_calm_time>
          </pacing>
          <schedule_entries>
            <schedule_entry>
              <day>Monday</day>
              <min_count>5</min_count>
              <time>02:30</time>
              <max_count>10</max_count>
            </schedule_entry>
            <schedule_entry>
              <day>Friday</day>
              <min_count>6</min_count>
              <time>14:00</time>
              <max_count>11</max_count>
            </schedule_entry>
          </schedule_entries>
          <alert_specific_params>
            <decision_threshold>50</decision_threshold>
            <voters_tag_predicate>predicate_29471830</voters_tag_predicate>
          </alert_specific_params>
        </elasticity_params>
        <name>name_767376035</name>
        <next_instance>
          <locked>false</locked>
          <links>
            <link rel="self" href="/api/clouds/926218062/instances/AMCD4DJMO9IA6"/>
            <link rel="cloud" href="/api/clouds/926218062"/>
            <link rel="deployment" href="/api/deployments/3"/>
            <link rel="server_template" href="/api/server_templates/1"/>
            <link rel="multi_cloud_image" href="/api/multi_cloud_images/2" inherited_source="server_template"/>
            <link rel="parent" href="/api/server_arrays/1"/>
            <link rel="volume_attachments" href="/api/clouds/926218062/instances/AMCD4DJMO9IA6/volume_attachments"/>
            <link rel="inputs" href="/api/clouds/926218062/instances/AMCD4DJMO9IA6/inputs"/>
            <link rel="monitoring_metrics" href="/api/clouds/926218062/instances/AMCD4DJMO9IA6/monitoring_metrics"/>
            <link rel="alerts" href="/api/clouds/926218062/instances/AMCD4DJMO9IA6/alerts"/>
          </links>
          <public_ip_addresses></public_ip_addresses>
          <created_at>2013/10/02 01:38:47 +0000</created_at>
          <private_ip_addresses></private_ip_addresses>
          <actions>
            <action rel="launch"/>
          </actions>
          <resource_uid>61a75390-2b03-11e3-9b39-6adbd225978a</resource_uid>
          <pricing_type>fixed</pricing_type>
          <name>name_767376035</name>
          <state>inactive</state>
          <updated_at>2013/10/02 01:38:47 +0000</updated_at>
        </next_instance>
        <instances_count>0</instances_count>
        <state>enabled</state>
      </server_array>
      <server_array>
        <links>
          <link rel="self" href="/api/server_arrays/2"/>
          <link rel="deployment" href="/api/deployments/10"/>
          <link rel="current_instances" href="/api/server_arrays/2/current_instances"/>
          <link rel="next_instance" href="/api/clouds/929741027/instances/69269QITG37J3"/>
          <link rel="alert_specs" href="/api/server_arrays/2/alert_specs"/>
          <link rel="alerts" href="/api/server_arrays/2/alerts"/>
        </links>
        <description>description_1456710297</description>
        <actions>
          <action rel="clone"/>
        </actions>
        <array_type>alert</array_type>
        <elasticity_params>
          <bounds>
            <min_count>2</min_count>
            <max_count>3</max_count>
          </bounds>
          <pacing>
            <resize_up_by>10</resize_up_by>
            <resize_down_by>5</resize_down_by>
            <resize_calm_time>6</resize_calm_time>
          </pacing>
          <schedule_entries>
            <schedule_entry>
              <day>Monday</day>
              <min_count>5</min_count>
              <time>02:30</time>
              <max_count>10</max_count>
            </schedule_entry>
            <schedule_entry>
              <day>Friday</day>
              <min_count>6</min_count>
              <time>14:00</time>
              <max_count>11</max_count>
            </schedule_entry>
          </schedule_entries>
          <alert_specific_params>
            <decision_threshold>50</decision_threshold>
            <voters_tag_predicate>predicate_2359006943</voters_tag_predicate>
          </alert_specific_params>
        </elasticity_params>
        <name>name_974414716</name>
        <instances_count>0</instances_count>
        <state>enabled</state>
      </server_array>
    </server_arrays>
    '''

    static def SERVER_ARRAY_INSTANCES = '''
    <instances>
      <instance>
        <private_ip_addresses>
          <private_ip_address>10.170.250.134</private_ip_address>
        </private_ip_addresses>
        <resource_uid>i-ac4193f1</resource_uid>
        <updated_at>2013/12/12 16:09:04 +0000</updated_at>
        <state>operational</state>
        <actions>
          <action rel="terminate"/>
          <action rel="reboot"/>
          <action rel="run_executable"/>
          <action rel="lock"/>
          <action rel="unlock"/>
        </actions>
        <created_at>2013/12/12 16:05:45 +0000</created_at>
        <name>RightScale Linux Server RL 5.7 #5</name>
        <locked>false</locked>
        <pricing_type>fixed</pricing_type>
        <public_ip_addresses>
          <public_ip_address>54.219.192.223</public_ip_address>
        </public_ip_addresses>
        <links>
          <link rel="self" href="/api/clouds/3/instances/6S5099DHD3JAT"/>
          <link rel="cloud" href="/api/clouds/3"/>
          <link rel="deployment" href="/api/deployments/439858003"/>
          <link rel="server_template" href="/api/server_templates/322789003"/>
          <link rel="multi_cloud_image" href="/api/multi_cloud_images/358557003"/>
          <link rel="parent" href="/api/server_arrays/1"/>
          <link rel="volume_attachments" href="/api/clouds/3/instances/6S5099DHD3JAT/volume_attachments"/>
          <link rel="inputs" href="/api/clouds/3/instances/6S5099DHD3JAT/inputs"/>
          <link rel="monitoring_metrics" href="/api/clouds/3/instances/6S5099DHD3JAT/monitoring_metrics"/>
          <link rel="alerts" href="/api/clouds/3/instances/6S5099DHD3JAT/alerts"/>
        </links>
      </instance>
    </instances>
    '''
static def SERVER_TEMPLATES = '''
    <server_templates>
      <server_template>
        <links>
          <link rel="self" href="/api/server_templates/1"/>
          <link rel="multi_cloud_images" href="/api/server_templates/1/multi_cloud_images"/>
          <link rel="default_multi_cloud_image" href="/api/multi_cloud_images/2"/>
          <link rel="inputs" href="/api/server_templates/1/inputs"/>
          <link rel="alert_specs" href="/api/server_templates/1/alert_specs"/>
          <link rel="runnable_bindings" href="/api/server_templates/1/runnable_bindings"/>
          <link rel="cookbook_attachments" href="/api/server_templates/1/cookbook_attachments"/>
        </links>
        <description>DESCRIPTION_3633115567</description>
        <actions>
          <action rel="commit"/>
          <action rel="clone"/>
          <action rel="resolve"/>
          <action rel="swap_repository"/>
          <action rel="detect_changes_in_head"/>
        </actions>
        <revision>0</revision>
        <name>NICKNAME_1948317086</name>
      </server_template>
      <server_template>
        <links>
          <link rel="self" href="/api/server_templates/5"/>
          <link rel="multi_cloud_images" href="/api/server_templates/5/multi_cloud_images"/>
          <link rel="default_multi_cloud_image" href="/api/multi_cloud_images/4"/>
          <link rel="inputs" href="/api/server_templates/5/inputs"/>
          <link rel="alert_specs" href="/api/server_templates/5/alert_specs"/>
          <link rel="runnable_bindings" href="/api/server_templates/5/runnable_bindings"/>
          <link rel="cookbook_attachments" href="/api/server_templates/5/cookbook_attachments"/>
        </links>
        <description>DESCRIPTION_3223207047</description>
        <actions>
          <action rel="commit"/>
          <action rel="clone"/>
          <action rel="resolve"/>
          <action rel="swap_repository"/>
          <action rel="detect_changes_in_head"/>
        </actions>
        <revision>0</revision>
        <name>NICKNAME_2529968303</name>
      </server_template>
    </server_templates>
    '''
def static SUBNETS = '''
    <subnets>
      <subnet>
        <links>
          <link rel="self" href="/api/clouds/926218062/subnets/ABC2907385845DEF"/>
          <link rel="datacenter" href="/api/clouds/926218062/datacenters/ABC3687362228DEF"/>
        </links>
        <description>subnet2729453100</description>
        <visibility>public</visibility>
        <resource_uid>i-920789752</resource_uid>
        <is_default>false</is_default>
        <name>some_name</name>
        <cidr_block></cidr_block>
        <state>pending</state>
      </subnet>
      <subnet>
        <links>
          <link rel="self" href="/api/clouds/929741027/subnets/ABC3841167812DEF"/>
          <link rel="datacenter" href="/api/clouds/929741027/datacenters/ABC587826053DEF"/>
        </links>
        <description>subnet3507842613</description>
        <visibility>public</visibility>
        <resource_uid>i-2694391650</resource_uid>
        <is_default>false</is_default>
        <name>some_name</name>
        <cidr_block></cidr_block>
        <state>pending</state>
      </subnet>
    </subnets>
   '''
def static TAGS = '''
    <resource_tags>
      <resource_tag>
        <links>
          <link rel="resource" href="/api/servers/10"/>
        </links>
        <actions></actions>
        <tags>
          <tag>
            <name>color:red=false</name>
          </tag>
          <tag>
            <name>color:blue=true</name>
          </tag>
        </tags>
      </resource_tag>
      <resource_tag>
        <links>
          <link rel="resource" href="/api/servers/20"/>
        </links>
        <actions></actions>
        <tags>
          <tag>
            <name>color:red=true</name>
          </tag>
          <tag>
            <name>color:blue=false</name>
          </tag>
        </tags>
      </resource_tag>
    </resource_tags>
  '''

    def static SSH_KEYS = '''
    <ssh_keys>
      <ssh_key>
        <links>
          <link rel="self" href="/api/clouds/926218062/ssh_keys/ABC3518369342DEF"/>
          <link rel="cloud" href="/api/clouds/926218062"/>
        </links>
        <actions></actions>
        <resource_uid>resource_4149218284</resource_uid>
      </ssh_key>
      <ssh_key>
        <links>
          <link rel="self" href="/api/clouds/929741027/ssh_keys/ABC2568952498DEF"/>
          <link rel="cloud" href="/api/clouds/929741027"/>
        </links>
        <actions></actions>
        <resource_uid>resource_913473243</resource_uid>
      </ssh_key>
    </ssh_keys>
    '''
}
