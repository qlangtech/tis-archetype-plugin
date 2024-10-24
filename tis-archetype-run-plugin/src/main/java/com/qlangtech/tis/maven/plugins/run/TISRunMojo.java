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
import java.net.URL;
import java.util.List;

import com.google.common.collect.Lists;
import com.qlangtech.tis.maven.plugins.archetype.ArchetypeCommon;
import com.qlangtech.tis.maven.plugins.run.InitPreDependencyRes.SubContext;
import com.qlangtech.tis.web.start.TISAppClassLoader;
import com.qlangtech.tis.web.start.TisApp;
import com.qlangtech.tis.web.start.TisAppLaunch;
import com.qlangtech.tis.web.start.TisRunMode;
import com.qlangtech.tis.web.start.TisSubModule;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-06-07 16:16
 **/
@Mojo(name = "run", requiresProject = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class TISRunMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "tis.version", required = true)
    public String tisVersion;

    @Parameter(property = "tis.port", required = false)
    public String tisRunPort;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ArchetypeCommon.initSysParams();

        InitPreDependencyRes initPreDependencyRes = new InitPreDependencyRes(tisVersion, this.getLog());
        getLog().info("local uber dir:" + initPreDependencyRes.getTisUberDir());
        System.setProperty(
                TisApp.KEY_WEB_ROOT_DIR,
                initPreDependencyRes.getTisUberDir().toPath().normalize().toString());
        // "tis-assemble.tar.gz", "ng-tis.tar.gz"
        for (SubContext pkgName : Lists.newArrayList(
                new SubContext("tis.tar.gz", TisSubModule.TIS_CONSOLE.moduleName, (entry) -> {
                    return StringUtils.substringAfter(entry.getName(), "/");
                }),
                new SubContext("tis-assemble.tar.gz", TisSubModule.TIS_ASSEMBLE.moduleName, (entry) -> {
                    return StringUtils.substringAfter(entry.getName(), "/");
                }),
                new SubContext("ng-tis.tar.gz", "root/webapp"),
                new SubContext("tis-data.tar.gz", "../" + ArchetypeCommon.UBER_DATA_DIR))) {
            initPreDependencyRes.downloads(pkgName);
        }
        if (StringUtils.isNotBlank(tisRunPort)) {
            System.setProperty(TisAppLaunch.KEY_TIS_LAUNCH_PORT, String.valueOf(Integer.parseInt(tisRunPort)));
            this.getLog().info("reset the http launch port:" + tisRunPort);
        }
        TisAppLaunch.get().setRunMode(TisRunMode.Standalone);
        try {
            TisApp.launchTISApp(createParentClassloader(), new String[] {});
        } catch (MojoFailureException e) {
            throw e;
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    private TISAppClassLoader createParentClassloader() throws MojoFailureException, MojoExecutionException {
        try {
            List<String> compileClasspathElements = this.project.getCompileClasspathElements();
            if (CollectionUtils.isEmpty(compileClasspathElements)) {
                throw new MojoExecutionException("compileClasspathElements can not be empty");
            }
            URL[] urls = new URL[compileClasspathElements.size()];
            int index = 0;
            for (String cp : compileClasspathElements) {
                urls[index++] = (new File(cp)).toURI().toURL();
            }
            return new TISAppClassLoader("root", Thread.currentThread().getContextClassLoader(), urls);
        } catch (Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }
}
