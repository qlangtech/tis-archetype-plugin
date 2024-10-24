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
package com.qlangtech.tis.maven.plugins.run;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.qlangtech.tis.extension.model.UpdateCenterResource;
import com.qlangtech.tis.manage.common.ConfigFileContext.StreamProcess;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.maven.plugins.archetype.ArchetypeCommon;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-06-08 23:16
 **/
public class InitPreDependencyRes {
    final File tmpDir;
    private String tisVersion;
    private final Log log;

    public InitPreDependencyRes(String tisVersion, Log log) {
        this.log = Objects.requireNonNull(log, "log can not be null");
        this.tmpDir = ArchetypeCommon.getTmpDir();
        this.tisVersion = tisVersion;
    }

    public File getTisUberDir() {
        return new File(tmpDir, "tis-uber");
    }

    public void downloads(SubContext pkg) {
        File pkgFile = new File(tmpDir, pkg.repoPkgName);
        URL pkgUrl = null;
        if (!pkgFile.exists()) {
            pkgUrl = UpdateCenterResource.getTISTarPkg(pkg.repoPkgName);
            HttpUtils.get(pkgUrl, new StreamProcess<Void>() {
                @Override
                public Void p(int status, InputStream stream, Map<String, List<String>> headerFields) {
                    write2file(stream, pkgFile);
                    return null;
                }
            });
        }
        File localDir = new File(getTisUberDir(), pkg.localPkgName);
        File cpSuccessToken = new File(localDir, "cp_success");
        if (!localDir.exists() || !cpSuccessToken.exists()) {

            TarArchiveEntry entry = null;
            File targetFile = null;
            try (InputStream reader = Files.newInputStream(pkgFile.toPath())) {
                TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(reader));
                while ((entry = tarInput.getNextTarEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    targetFile = new File(localDir, pkg.entryPathCreate.apply(entry));

                    write2file(tarInput, targetFile);
                }
                FileUtils.touch(cpSuccessToken);
                log.info("download:" + pkgUrl + " to local dir:" + localDir);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void write2file(InputStream stream, File pkgFile) {
        try (OutputStream out = FileUtils.openOutputStream(pkgFile)) {
            IOUtils.copy(stream, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class SubContext {
        public final String repoPkgName;
        public final String localPkgName;
        public final Function<TarArchiveEntry, String> entryPathCreate;

        public SubContext(String repoPkgName, String localPkgName) {
            this(repoPkgName, localPkgName, (entry) -> entry.getName());
        }

        public SubContext(String repoPkgName, String localPkgName, Function<TarArchiveEntry, String> entryPathCreate) {
            this.repoPkgName = repoPkgName;
            this.localPkgName = localPkgName;
            this.entryPathCreate = entryPathCreate;
        }
    }
}
