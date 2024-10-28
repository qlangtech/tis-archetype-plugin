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
package org.jenkinsci.maven.plugins.hpi;

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Default and currently the only implementation of {@link PluginWorkspaceMap}
 *https://maven.apache.org/maven-jsr330.html
 * @author Jesse Glick
 * @author Kohsuke Kawaguchi
 */
@Named
@Singleton
public class PluginWorkspaceMapImpl implements PluginWorkspaceMap {
    private final File mapFile;

    public PluginWorkspaceMapImpl(File mapFile) {
        this.mapFile = mapFile;
    }

    public PluginWorkspaceMapImpl() {
        this.mapFile = new File(System.getProperty("user.home"), ".jenkins-hpl-map");
    }

    private Properties loadMap() throws IOException {
        Properties p = new Properties();
        if (mapFile.isFile()) {
            try (InputStream is = new FileInputStream(mapFile)) {
                p.load(is);
            } catch (IllegalArgumentException x) {
                throw new IOException("Malformed " + mapFile + ": " + x, x);
            }
        }
        return p;
    }

    @Override
    public File read(String id) throws IOException {
        for (Map.Entry<Object, Object> entry : loadMap().entrySet()) {
            if (entry.getValue().equals(id)) {
                String path = (String) entry.getKey();
                File f = new File(path);
                if (f.exists()) {
                    return f;
                }
            }
        }
        return null;
    }

    @Override
    public void write(String id, File f) throws IOException {
        Properties p = loadMap();
        p.setProperty(f.getAbsolutePath(), id);
        try (OutputStream os = new FileOutputStream(mapFile)) {
            p.store(os, " List of development files for Jenkins plugins that have been built.");
        }
    }
}
