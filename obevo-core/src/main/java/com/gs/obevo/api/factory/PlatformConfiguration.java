/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gs.obevo.api.factory;

import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.api.platform.Platform;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains any common default configurations used by all Obevo implementations.
 */
public class PlatformConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(PlatformConfiguration.class);
    private static final PlatformConfiguration INSTANCE = new PlatformConfiguration();

    private final Config config;
    private final String toolVersion;
    private final ImmutableMap<String, Integer> featureToggleVersions;
    private final String sourceEncoding;
    private final Config platformConfigs;
    private final ImmutableMap<String, String> dbPlatformMap;

    public static PlatformConfiguration getInstance() {
        return INSTANCE;
    }

    protected PlatformConfiguration() {
        this.config = ConfigFactory.parseProperties(new PlatformConfigReader().readPlatformProperties(getConfigPackages()));
        this.toolVersion = config.getString("tool.version");
        this.featureToggleVersions = createFeatureToggleVersions();
        this.sourceEncoding = this.getConfig().getString("sourceEncoding");
        this.platformConfigs = getPlatformConfigs();
        this.dbPlatformMap = getDbPlatformMap();
    }

    private Config getPlatformConfigs() {
        if (this.getConfig().hasPath("db")) {
            return this.getConfig().getConfig("db").getConfig("platforms");
        }
        return ConfigFactory.empty();
    }

    public Platform valueOf(String dbPlatformStr) {
        try {
            String resolvedDbPlatformClass = dbPlatformMap.get(dbPlatformStr);
            if (resolvedDbPlatformClass == null) {
                resolvedDbPlatformClass = dbPlatformStr;
            }
            return (Platform) Class.forName(resolvedDbPlatformClass).newInstance();
        } catch (InstantiationException e) {
            throw new DeployerRuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new DeployerRuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new DeployerRuntimeException(e);
        }
    }

    /**
     * Returns the default name-to-platform mappings. We put this in a separate protected method to allow external
     * distributions to override these values as needed.
     */
    private ImmutableMap<String, String> getDbPlatformMap() {
        MutableMap<String, String> platformByName = Maps.mutable.empty();

        for (String platformName : platformConfigs.root().keySet()) {
            String platformClass = getPlatformConfig(platformName).getString("class");
            platformByName.put(platformName, platformClass);
            LOG.debug("Registering platform {} at class {}", platformName, platformClass);
        }

        return platformByName.toImmutable();
    }

    public Config getPlatformConfig(String platformName) {
        return platformConfigs.getConfig(platformName);
    }

    /**
     * Returns the resources to read from to populate this instance. To be overriden by child implementations.
     */
    protected ImmutableList<String> getConfigPackages() {
        return Lists.immutable.of("com/gs/obevo/config", "com/gs/obevo/db/config");
    }

    protected Config getConfig() {
        return config;
    }

    /**
     * Returns the product name to use for output.
     */
    public String getToolName() {
        return "obevo";  // TODO read this from configuration to allow different distributions to name this accordingly
    }

    /**
     * Returns the product version to use for output.
     */
    public String getToolVersion() {
        return toolVersion;
    }

    public int getFeatureToggleVersion(String featureToggleName) {
        return featureToggleVersions.getIfAbsentValue(featureToggleName, 0);
    }

    private ImmutableMap<String, Integer> createFeatureToggleVersions() {
        if (!this.getConfig().hasPath("featureToggles")) {
            return Maps.immutable.empty();
        }
        final Config attrConfig = this.getConfig().getConfig("featureToggles");
        return SetAdapter.adapt(attrConfig.root().keySet()).toMap(Functions.<String>getPassThru(), new Function<String, Integer>() {
            @Override
            public Integer valueOf(String featureToggleName) {
                return attrConfig.getConfig(featureToggleName).getInt("defaultVersion");
            }
        }).toImmutable();
    }

    public String getSourceEncoding() {
        return sourceEncoding;
    }
}
