
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

    public static final String EMAIL = "email";
    public static final String PASSWORD = "password";
    public static final String ACCOUNT = "account";
    public static final String REFRESH_INTERVAL = "refreshInterval";
    public static final String USERNAME = "username";

    /**
     * Default constructor.
     * @param framework
     * @return new instance.
     */
    public ResourceModelSourceFactory(final Framework framework) {
        this.framework = framework;
    }

    public ResourceModelSource createResourceModelSource(final Properties properties) throws ConfigurationException {
        final RightscaleNodes modelSource = new RightscaleNodes(properties);
        modelSource.validate();
        return modelSource;
    }

    /**
     * Plugin configuration properties.
     */
    static Description DESC = DescriptionBuilder.builder()
            .name(PROVIDER_NAME)
            .title("RightScale Servers")
            .description("Generates nodes from a list of servers in your RightScale account.")
            .property(PropertyUtil.string(EMAIL, "Email", "Email address for RightScale User", true, null))
            .property(PropertyUtil.string(PASSWORD, "Password", "RightScale Password", true, null))
            .property(PropertyUtil.string(ACCOUNT, "Account", "RightScale Account", true, null))
            .property(PropertyUtil.integer(REFRESH_INTERVAL, "Refresh Interval",
                    "Minimum time in seconds between API requests to RightScale (default is 60)", false, "60"))
            .property(PropertyUtil.string(ENDPOINT, "Endpoint", "RightScale  API Endpoint URL. Must support API v1.5", false, "https://us-3.rightscale.com"))
            .property(PropertyUtil.string(USERNAME, "Username", "Username for remote command execution", true, null))
            .build();

    public Description getDescription() {
        return DESC;
    }
}
