package com.qlangtech.tis.maven.plugins.archetype;

import org.apache.commons.lang.StringUtils;

import java.util.Optional;

import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-06-06 16:02
 **/
public class Coord {

    private final String groupId;
    private final String artifactId;
    private final String version;
    //public final String packaging;
    private final Optional<PluginClassifier> classifier;

    public static Coord parse(String gav) {
        String[] tuple = StringUtils.split(gav, ":");
        if (tuple.length != 3 && tuple.length != 4) {
            throw new IllegalStateException("illegal gav:" + gav);
        }
        Coord coord = new Coord(tuple[0], tuple[1], tuple[2], tuple.length == 4 ? Optional.of(PluginClassifier.create(tuple[3])) : Optional.empty());
        return coord;
    }

    private Coord(String groupId, String artifactId, String version, Optional<PluginClassifier> classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }
}
