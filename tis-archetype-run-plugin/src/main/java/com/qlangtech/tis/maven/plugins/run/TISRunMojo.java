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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import com.google.common.collect.Lists;
import com.qlangtech.tis.extension.PluginManager;
import com.qlangtech.tis.extension.PluginStrategy;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.maven.plugins.archetype.ArchetypeCommon;
import com.qlangtech.tis.maven.plugins.run.InitPreDependencyRes.SubContext;
import com.qlangtech.tis.solrj.util.ZkUtils;
import com.qlangtech.tis.web.start.TISAppClassLoader;
import com.qlangtech.tis.web.start.TisApp;
import com.qlangtech.tis.web.start.TisAppLaunch;
import com.qlangtech.tis.web.start.TisRunMode;
import com.qlangtech.tis.web.start.TisSubModule;
import hudson.util.VersionNumber;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.jenkinsci.maven.plugins.hpi.MavenArtifact;
import org.jenkinsci.maven.plugins.hpi.PluginWorkspaceMap;
import org.twdata.maven.mojoexecutor.MojoExecutor;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-06-07 16:16
 **/
@Mojo(name = "run", requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.COMPILE)
public class TISRunMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    protected MavenSession session;

    /**
     * Single directory for extra files to include in the WAR.
     */
    @Parameter(defaultValue = "${basedir}/src/main/webapp")
    protected File warSourceDirectory;

    @Parameter(property = "tis.version", required = true)
    public String tisVersion;

    @Parameter(property = "tis.port", required = false)
    public String tisRunPort;

    @Component
    protected RepositorySystem repositorySystem;

    @Component
    protected ArtifactFactory artifactFactory;

    @Component
    protected PluginWorkspaceMap pluginWorkspaceMap;

    @Component
    private ProjectDependenciesResolver dependenciesResolver;

    @Component
    protected ProjectBuilder projectBuilder;

    @Component
    private BuildPluginManager pluginManager;

    /**
     * Decides the level of dependency resolution.
     * <p>
     * This controls what plugins are made available to the
     * running Jenkins.
     */
    @Parameter(defaultValue = "test")
    protected String dependencyResolution;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ArchetypeCommon.initSysParams();
        getProject().setArtifacts(resolveDependencies(dependencyResolution));
        ZkUtils.localhostValidator = (ip) -> {
            // 不进行地址是否是127.0.0.1的校验
            return;
        };
        initPreDependencyRes();

        File pluginsDir = new File(Config.getDataDir(), Config.LIB_PLUGINS_PATH);
        try {
            Files.createDirectories(pluginsDir.toPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create directories for '" + pluginsDir + "'", e);
        }
        generateHpl();
        // copy other dependency Jenkins plugins
        try {
            for (MavenArtifact a : getProjectArtifacts()) {
                if (!a.isPluginBestEffort(getLog())) {
                    continue;
                }

                // find corresponding .hpi file
                Artifact hpi =
                        artifactFactory.createArtifact(a.getGroupId(), a.getArtifactId(), a.getVersion(), null, "tpi");
                hpi = MavenArtifact.resolveArtifact(hpi, project, session, repositorySystem);

                // check recursive dependency. this is a rare case that happens when we split out some things from the
                // core into a plugin
                if (hasSameGavAsProject(hpi)) {
                    continue;
                }

                if (hpi.getFile().isDirectory()) {
                    throw new UnsupportedOperationException(
                            hpi.getFile() + " is a directory and not packaged yet. this isn't supported");
                }

                File upstreamHpl = pluginWorkspaceMap.read(hpi.getId());
                String actualArtifactId = a.getActualArtifactId();
                if (actualArtifactId == null) {
                    throw new MojoExecutionException(
                            "Failed to load actual artifactId from " + a + " ~ " + a.getFile());
                }
                if (upstreamHpl != null) {
                    copyHpl(upstreamHpl, pluginsDir, actualArtifactId);
                } else {
                    copyPlugin(hpi.getFile(), pluginsDir, actualArtifactId);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to copy dependency plugin", e);
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

    private void copyPlugin(File src, File pluginsDir, String shortName) throws IOException {

        File dst = new File(pluginsDir, shortName + PluginManager.PACAKGE_TPI_EXTENSION);
        // File hpi = new File(pluginsDir, shortName + PluginManager.PACAKGE_TPI_EXTENSION);
        //        if (Files.isRegularFile(hpi.toPath())) {
        //            getLog().warn("Moving historical " + hpi + " to *.jpi");
        //            Files.move(hpi.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        //        }
        VersionNumber dstV = versionOfPlugin(dst);
        if (versionOfPlugin(src).compareTo(dstV) <= 0) {
            getLog().info("will not overwrite " + dst + " with " + src + " because " + dstV + " is newer");
            return;
        }
        getLog().info("Copying dependency Jenkins plugin " + src);
        FileUtils.copyFile(src, dst);
        // TODO skip .pinned file creation if Jenkins version is >= 2.0
        // pin the dependency plugin, so that even if a different version of the same plugin is bundled to Jenkins,
        // we still use the plugin as specified by the POM of the plugin.
        //        Files.writeString(pluginsDir.toPath().resolve(shortName + ".jpi.pinned"), "pinned",
        // StandardCharsets.US_ASCII);
        //        Files.deleteIfExists(
        //                new File(pluginsDir, shortName + ".jpl").toPath()); // in case we used to have a snapshot
        // dependency
    }

    private VersionNumber versionOfPlugin(File p) throws IOException {
        if (!p.isFile()) {
            return new VersionNumber("0.0");
        }
        try (JarFile j = new JarFile(p)) {
            Attributes attrs = j.getManifest().getMainAttributes();

            String v = attrs.getValue("Plugin-Version");
            if (v == null) {
                throw new IOException("no Plugin-Version in " + p);
            }
            try {
                return new TISVersionNumber(v, Long.parseLong(attrs.getValue(PluginStrategy.KEY_LAST_MODIFY_TIME)));
            } catch (IllegalArgumentException x) {
                throw new IOException("malformed Plugin-Version in " + p + ": " + x, x);
            }
        } catch (IOException x) {
            throw new IOException("not a valid JarFile: " + p, x);
        }
    }

    private void copyHpl(File src, File pluginsDir, String shortName) throws IOException {
        throw new UnsupportedEncodingException();
        //        File dst = new File(pluginsDir, shortName + ".jpl");
        //        getLog().info("Copying snapshot dependency Jenkins plugin " + src);
        //        FileUtils.copyFile(src, dst);
        //        Files.writeString(pluginsDir.toPath().resolve(shortName + ".jpi.pinned"), "pinned",
        // StandardCharsets.US_ASCII);
    }

    private boolean hasSameGavAsProject(Artifact a) {
        return getProject().getGroupId().equals(a.getGroupId())
                && getProject().getArtifactId().equals(a.getArtifactId())
                && getProject().getVersion().equals(a.getVersion());
    }

    public Set<MavenArtifact> getProjectArtifacts() {
        Set<MavenArtifact> r = new HashSet<>();
        for (Artifact a : getProject().getArtifacts()) {
            r.add(wrap(a));
        }
        return r;
    }

    protected MavenArtifact wrap(Artifact a) {
        return new MavenArtifact(a, repositorySystem, artifactFactory, projectBuilder, session, project);
    }

    /**
     * Create a dot-hpl file.
     */
    private void generateHpl() throws MojoExecutionException {
        String tisDataDir = Config.getDataDir().toPath().normalize().toString();
        String jenkinsCoreId = null;
        boolean pluginFirstClassLoader = false;
        String maskClasses = null;
        MojoExecutor.executeMojo(
                MojoExecutor.plugin(
                        MojoExecutor.groupId("com.qlangtech.tis"), MojoExecutor.artifactId("maven-tpi-plugin")),
                MojoExecutor.goal("hpl"),
                MojoExecutor.configuration(
                        // MojoExecutor.element("classpathDependentExcludes",);
                        MojoExecutor.element(MojoExecutor.name("tisDataDir"), tisDataDir),
                        MojoExecutor.element(MojoExecutor.name("pluginName"), project.getName()),
                        MojoExecutor.element(MojoExecutor.name("warSourceDirectory"), warSourceDirectory.toString()),
                        //  MojoExecutor.element(MojoExecutor.name("jenkinsCoreId"), jenkinsCoreId),
                        MojoExecutor.element(
                                MojoExecutor.name("pluginFirstClassLoader"), Boolean.toString(pluginFirstClassLoader)),
                        MojoExecutor.element(MojoExecutor.name("maskClasses"), maskClasses)),
                MojoExecutor.executionEnvironment(project, session, pluginManager));
    }

    /**
     * Performs the equivalent of "@requiresDependencyResolution" mojo attribute,
     * so that we can choose the scope at runtime.
     * <p>
     * // @see org.apache.maven.lifecycle.internal.LifecycleDependencyResolver#getDependencies(MavenProject, java.util.Collection, java.util.Collection,
     * org.apache.maven.execution.MavenSession, boolean, java.util.Set)
     */
    protected Set<Artifact> resolveDependencies(String scope) throws MojoExecutionException {
        try {
            DependencyResolutionRequest request =
                    new DefaultDependencyResolutionRequest(getProject(), session.getRepositorySession());
            request.setResolutionFilter(getDependencyFilter(scope));
            DependencyResolutionResult result = dependenciesResolver.resolve(request);

            Set<Artifact> artifacts = new LinkedHashSet<>();
            if (result.getDependencyGraph() != null
                    && !result.getDependencyGraph().getChildren().isEmpty()) {
                RepositoryUtils.toArtifacts(
                        artifacts,
                        result.getDependencyGraph().getChildren(),
                        Collections.singletonList(getProject().getArtifact().getId()),
                        request.getResolutionFilter());
            }
            return artifacts;
        } catch (DependencyResolutionException e) {
            throw new MojoExecutionException("Unable to copy dependency plugin", e);
        }
    }

    private static DependencyFilter getDependencyFilter(String scope) {
        switch (scope) {
            case Artifact.SCOPE_COMPILE:
                return new ScopeDependencyFilter(Artifact.SCOPE_RUNTIME, Artifact.SCOPE_TEST);
            case Artifact.SCOPE_RUNTIME:
                return new ScopeDependencyFilter(Artifact.SCOPE_SYSTEM, Artifact.SCOPE_PROVIDED, Artifact.SCOPE_TEST);
            case Artifact.SCOPE_TEST:
                return null;
            default:
                throw new IllegalArgumentException("unexpected scope: " + scope);
        }
    }

    protected MavenProject getProject() {
        return project;
    }

    private InitPreDependencyRes initPreDependencyRes;

    private void initPreDependencyRes() {
        this.initPreDependencyRes = new InitPreDependencyRes(tisVersion, this.getLog());
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
    }

    private TISAppClassLoader createParentClassloader() throws MojoFailureException, MojoExecutionException {
        try {
            // List<String> compileClasspathElements = this.project.getCompileClasspathElements();
            //            if (CollectionUtils.isEmpty(compileClasspathElements)) {
            //                throw new MojoExecutionException("compileClasspathElements can not be empty");
            //            }
            URL[] urls = new URL[0];
            //            int index = 0;
            //            for (String cp : compileClasspathElements) {
            //                urls[index++] = (new File(cp)).toURI().toURL();
            //            }
            return new TISAppClassLoader("root", this.getClass().getClassLoader(), urls);
        } catch (Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }
}
