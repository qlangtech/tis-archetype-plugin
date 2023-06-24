package com.qlangtech.tis.maven.plugins.archetype;

import org.apache.commons.compress.utils.Lists;
import org.apache.velocity.VelocityContext;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.internal.util.Sets;
import com.qlangtech.tis.maven.plugins.archetype.GenerateArchetypeMojo.ExtendPluginMeta;
import com.qlangtech.tis.plugin.PluginCategory;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-06-06 11:50
 **/
public class ArchetypeProject {
    private final List<ExtendPlugin> extendPlugins;
    private final GenerateArchetypeMojo archetypeMojo;

    public static final Pattern PATTERN_INCR_NAME = Pattern.compile("tis-flink-([a-z,0-9,\\-]+)-plugin");
    public static final Pattern PATTERN_NONE_NAME = Pattern.compile("tis-([a-z,0-9,\\-]+)-plugin");
    public static final Pattern PATTERN_BATCH_NAME = Pattern.compile("tis-datax-([a-z,0-9,\\-]+)-plugin");

    public ArchetypeProject(GenerateArchetypeMojo archetypeMojo, List<ExtendPlugin> extendPlugins) {
        this.extendPlugins = extendPlugins;
        this.archetypeMojo = archetypeMojo;
    }

    public void validate() {
        Set<PluginCategory> categories = Sets.newHashSet();
        for (ExtendPlugin extend : extendPlugins) {
            categories.add(extend.getPluginMeta().category);
        }
        if (categories.size() > 1) {
            if (categories.contains(PluginCategory.BATCH) && categories.contains(PluginCategory.INCR)) {
                // 原则上一个插件工程中不能同时实现批和流的功能
                throw new IllegalStateException("extned batch and incr can not mix in one plugin");
            }
        }

        ExtendPluginMeta pluginMeta = null;
        for (ExtendPlugin extend : extendPlugins) {
            pluginMeta = extend.getPluginMeta();
            //  if (pluginMeta != null) {
            Pattern pattern = null;
            switch (pluginMeta.category) {
                case INCR:
                    pattern = PATTERN_INCR_NAME;
                    break;
                case NONE:
                    pattern = PATTERN_NONE_NAME;
                    break;
                case BATCH:
                    pattern = PATTERN_BATCH_NAME;
                    break;
                default:
                    throw new IllegalStateException("invalid category:" + pluginMeta.category);
            }
            Matcher matcher = pattern.matcher(archetypeMojo.artifactId);
            if (!matcher.matches()) {
                throw new IllegalStateException("artifactId:" + archetypeMojo.artifactId + " is invalid, shall be pattern of:" + pattern);
            }
        }
        // }
    }

    public String getProjectVersion() {
        return this.archetypeMojo.tisVersion;
    }

    private Coord parentPomArtifact;

    public Coord getParentPom() {

        if (this.parentPomArtifact == null) {
            PluginCategory cat = this.getCategory();
            switch (cat) {
                case BATCH:
//        <groupId>com.qlangtech.tis.plugins</groupId>
//        <artifactId>tis-datax</artifactId>
//        <version>${data.projectVersion}</version>
                    this.parentPomArtifact = Coord.parse("com.qlangtech.tis.plugins:tis-datax:" + getProjectVersion());
                    break;
                case NONE:
                    this.parentPomArtifact = Coord.parse("com.qlangtech.tis.plugins:tis-plugin-parent:" + getProjectVersion());
                    break;
                case INCR:
                    this.parentPomArtifact = Coord.parse("com.qlangtech.tis.plugins:tis-incr:" + getProjectVersion());
                    break;
                default:
                    throw new IllegalStateException("illegal category:" + cat);
            }
        }

        return this.parentPomArtifact;
    }

    public boolean isBatch() {
        return getCategory() == PluginCategory.BATCH;
    }

    public boolean isIncr() {
        return getCategory() == PluginCategory.INCR;
    }

    private PluginCategory getCategory() {
        ExtendPluginMeta pluginMeta = null;
        for (ExtendPlugin extend : extendPlugins) {
            if ((pluginMeta = extend.getPluginMeta()) != null) {
                if (pluginMeta.category != PluginCategory.NONE) {
                    return pluginMeta.category;
                }
            }
        }
        return PluginCategory.NONE;
    }

    public List<ExtendPlugin> getExtendPlugins() {
        return this.extendPlugins;
    }

    public List<Coord> getCoords() {
        List<Coord> coords = Lists.newArrayList();
        ExtendPluginMeta pluginMeta = null;
        Optional<String> gav = null;
        for (ExtendPlugin extend : extendPlugins) {
            pluginMeta = extend.getPluginMeta();
            gav = pluginMeta.getGav();
            if (gav.isPresent()) {
                coords.add(Coord.parse(gav.get()));
            }
        }
        return coords;
    }

    public File getDistDir() {
        File distDir = new File(new File("."), this.getNewProjectName());
        return distDir;
    }

    public String getNewProjectName() {
        return archetypeMojo.artifactId;
    }

    public VelocityContext createVelocityContext() {
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("data", this);
//        velocityContext.put(DataxUtils.DATAX_NAME, this.dataxName);
//        velocityContext.put("reader", reader);
//        velocityContext.put("writer", writer);
//        velocityContext.put("cfg", this.globalCfg);
        return velocityContext;
    }
}
