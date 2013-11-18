
package com.simplifyops.rundeck.plugin.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.resources.ResourceModelSource;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceFactory;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;

import java.util.*;


@Plugin(name="rightscale-nodes", service="ResourceModelSource")
public class RightscaleNodesFactory implements ResourceModelSourceFactory, Describable {
    public static final String PROVIDER_NAME = "rightscale-nodes";
    private Framework framework;

    public static final String ENDPOINT = "endpoint";

    public static final String RUNNING_ONLY = "runningOnly";
    public static final String EMAIL = "email";
    public static final String PASSWORD = "password";
    public static final String ACCOUNT = "account";
    public static final String REFRESH_INTERVAL = "refreshInterval";


    public ResourceModelSourceFactory(final Framework framework) {
        this.framework = framework;
    }

    public ResourceModelSource createResourceModelSource(final Properties properties) throws ConfigurationException {
        final RightscaleNodes modelSource = new RightscaleNodes(properties);
        modelSource.validate();
        return modelSource;
    }

    static Description DESC = DescriptionBuilder.builder()
            .name(PROVIDER_NAME)
            .title("Rigthscale Resource Model Source")
            .description("Generates nodes from a list of servers in your account via Rightscale API: /api/servers")
            .property(PropertyUtil.string(EMAIL, "Email", "Email address for Rightscale User", true, null))
            .property(PropertyUtil.string(PASSWORD, "Password", "Rightscale Password", true, null))
            .property(PropertyUtil.string(ACCOUNT, "Account", "Rightscale Account", true, null))
            .property(PropertyUtil.integer(REFRESH_INTERVAL, "Refresh Interval",
                    "Minimum time in seconds between API requests to Rightscale (default is 30)", false, "30"))
            .property(PropertyUtil.string(ENDPOINT, "Endpoint", "Rightscale  Endpoint, or blank for default", false, "https://us-3.rightscale.com"))
            .property(PropertyUtil.bool(RUNNING_ONLY, "Only Operational Instances",
                    "Include Operational state instances only. If false, all instances will be returned",
                    false, "true"))

            .build();

    public Description getDescription() {
        return DESC;
    }
}
