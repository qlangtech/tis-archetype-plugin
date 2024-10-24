/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.qlangtech.tis.maven.plugins.archetype;

import java.util.Optional;

import com.qlangtech.tis.maven.plugins.tpi.PluginClassifier;
import org.apache.commons.lang.StringUtils;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-06-06 16:02
 **/
public class Coord {

    private final String groupId;
    private final String artifactId;
    private final String version;
    // public final String packaging;
    private final Optional<PluginClassifier> classifier;

    public static Coord parse(String gav) {
        String[] tuple = StringUtils.split(gav, ":");
        if (tuple.length != 3 && tuple.length != 4) {
            throw new IllegalStateException("illegal gav:" + gav);
        }
        Coord coord = new Coord(
                tuple[0],
                tuple[1],
                tuple[2],
                tuple.length == 4 ? Optional.of(PluginClassifier.create(tuple[3])) : Optional.empty());
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
