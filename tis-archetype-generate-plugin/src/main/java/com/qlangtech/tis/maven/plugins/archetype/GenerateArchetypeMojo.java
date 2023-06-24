package com.qlangtech.tis.maven.plugins.archetype;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.velocity.VelocityContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.codehaus.plexus.velocity.VelocityComponent;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.qlangtech.tis.extension.model.FormValidation;
import com.qlangtech.tis.extension.model.IPluginCoord;
import com.qlangtech.tis.extension.model.UpdateCenter;
import com.qlangtech.tis.extension.model.UpdateCenterResource;
import com.qlangtech.tis.extension.model.UpdateSite;
import com.qlangtech.tis.extension.model.UpdateSite.Plugin;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.ConfigFileContext.StreamProcess;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.PluginCategory;
import com.qlangtech.tis.pubhook.common.RunEnvironment;

//import org.codehaus.plexus.component.annotations.Component;

/**
 * 生成TIS 插件的骨架代码
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-06-03 08:59
 **/
@Mojo(name = "generate", requiresProject = false)
@Execute(phase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateArchetypeMojo extends AbstractMojo implements ContextEnabled {

    @Parameter(property = "tis.version", required = true)
    public String tisVersion;

    @Parameter(property = "tis.extendpoint", required = true)
    public String extendPoint;

    @Parameter(property = "tis.artifactId", required = true)
    public String artifactId;

    @Component
    public VelocityComponent velocity;

    public ArchetypeProject getProjectModel() {
        List<ExtendPlugin> extendPlugins = ExtendPlugin.parse(this.extendPoint);
        ArchetypeProject projectModel = new ArchetypeProject(this, extendPlugins);
        return projectModel;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ArchetypeCommon.initSysParams();


        File tmpDir = ArchetypeCommon.getTmpDir();
        ArchetypeProject projectModel = getProjectModel();
        List<ExtendPlugin> extendPlugins = projectModel.getExtendPlugins();

        UpdateSite updateSite = UpdateSite.tisDftUpdateSite(tisVersion);
        // this.tisVersion;
        try {
            Future<FormValidation> f = updateSite.updateDirectly();
            if (f != null) {
                f.get(5, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        Map<String, ExtendPluginMeta> categoryMetas = getCategoryPluginMeta(tmpDir);
        ExtendPluginMeta pluginMeta = null;
        List<Plugin> plugins = updateSite.getPlugins((p) -> true);


        for (ExtendPlugin extend : extendPlugins) {

            boolean match = false;

            if (match = (pluginMeta = categoryMetas.get(extend.extend)) != null) {
                extend.setPluginMeta(pluginMeta);

            } else {


                find:
                for (Plugin plugin : plugins) {
                    for (Map.Entry<String, List<String>> entry : plugin.extendPoints.entrySet()) {
                        if (StringUtils.equals(entry.getKey(), extend.extend)) {
                            match = true;

                            pluginMeta = categoryMetas.get(extend.extend);
                            Objects.requireNonNull(pluginMeta, "extendPoint:" + extend.extend + " can not be null");
                            extend.setPluginMeta(pluginMeta);
                            // 扩展点在TIS core中吗？
                            // 在，则忽略
                            //   判断扩展点的类型
                            //     1. 增量
                            //     2. 批量
                            //     3. 不属于任何
                            // 不在，将本Plugin作为依赖


                            break find;
                        }
                        match = CollectionUtils.exists(entry.getValue(), (impl) -> {
                                    boolean equal = StringUtils.equals((String) impl, extend.extend);
                                    if (equal) {
                                        IPluginCoord coord = plugin.getTargetCoord(Optional.empty());
                                        // coord.getDownloadUrl();
                                        ExtendPluginMeta meta = new ExtendPluginMeta(extend.extend, PluginCategory.NONE);
                                        meta.gav = Optional.of(coord.getGav());
                                        extend.setPluginMeta(meta);
                                    }
                                    return equal;
                                }
                        );

                    }

                    if (match) {
                        break find;
                    }

                }
            }

            if (!match) {
                throw new IllegalStateException("extendPoint:" + extend.extend + " is not valid");
            }
        }
        projectModel.validate();
        // 开始生成

        File distDir = projectModel.getDistDir();// new File(new File("."), projectModel.getNewProjectName());
        try {
            if (distDir.exists()) {
                throw new MojoExecutionException("dist dir:" + distDir.getAbsolutePath() + " is exist can not create again");
            } else {
                FileUtils.forceMkdir(distDir);
            }


            // pom.xml
            VelocityContext mergeData = projectModel.createVelocityContext();
            StringWriter vwriter = new StringWriter();
            String templateName = "/template/pom.xml.vm";
            velocity.getEngine().mergeTemplate(templateName, TisUTF8.getName(), mergeData, vwriter);
            FileUtils.write(new File(distDir, "pom.xml"), vwriter.toString(), TisUTF8.get());

            //  System.out.println(vwriter.toString());

            // TestAll.java
            templateName = "/template/java/TestAll.java.vm";
            vwriter = new StringWriter();
            velocity.getEngine().mergeTemplate(templateName, TisUTF8.getName(), mergeData, vwriter);
            FileUtils.write(new File(distDir, "src/test/java/TestAll.java"), vwriter.toString(), TisUTF8.get());
            // System.out.println(vwriter.toString());
            // 确定工程名称

            // pom.xml


            // 生成 Java类目

            for (ExtendPlugin extend : extendPlugins) {

                final PluginClass newClassInfo = extend.getNewClassInfo(false);

                mergeData = extend.createVelocityContext();
                vwriter = new StringWriter();
                templateName = "/template/java/plugin.java.vm";
                velocity.getEngine().mergeTemplate(templateName, TisUTF8.getName(), mergeData, vwriter);
                FileUtils.write(new File(distDir, "src/main/java/" + newClassInfo.getFullClazzRelativePath()), vwriter.toString(), TisUTF8.get());
                //  System.out.println(vwriter.toString());

                // 生成对应的Test类
                final PluginClass newTestClassInfo = extend.getNewClassInfo(true);
                vwriter = new StringWriter();
                templateName = "/template/java/test-plugin.java.vm";
                velocity.getEngine().mergeTemplate(templateName, TisUTF8.getName(), mergeData, vwriter);
                FileUtils.write(new File(distDir, "src/test/java/" + newTestClassInfo.getFullClazzRelativePath()), vwriter.toString(), TisUTF8.get());

                // 生成props json
                FileUtils.write(new File(distDir, "src/main/resources/" + newClassInfo.getFullRelativePath("json")), "{\n}", TisUTF8.get());
                // 生成props md
                FileUtils.write(new File(distDir, "src/main/resources/" + newClassInfo.getFullRelativePath("md")), "", TisUTF8.get());
            }

            // 生成 Resources文件
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 生成 Test类骨架
//        for (ExtendPlugin extend : extendPlugins) {
//            mergeData = extend.createVelocityContext();
//
//        }

    }



//    public static final String UBER_DATA_DIR = "uber-data";
//
//    public static File getTmpDir() {
//        File tmpDir = null;
//        try {
//            tmpDir = (new File(FileUtils.getUserDirectory(), "tis_tmp"));
//            // tmpDir = ;
//            FileUtils.forceMkdir(tmpDir);
//            Config.setDataDir(new File(tmpDir, UBER_DATA_DIR + "/data").getAbsolutePath());
//            //  FileUtils.forceMkdir(new File(Config.getLibDir(), com.qlangtech.tis.TIS.KEY_TIS_PLUGIN_ROOT));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        return tmpDir;
//    }


    Map<String, ExtendPluginMeta> getCategoryPluginMeta(File dataDir) {

        return HttpUtils.get((UpdateCenterResource.getTISReleaseRes(
                tisVersion, UpdateCenterResource.KEY_UPDATE_SITE, UpdateCenter.PLUGIN_CATEGORIES_FILENAME))
                , new StreamProcess<Map<String, ExtendPluginMeta>>() {
                    @Override
                    public Map<String, ExtendPluginMeta> p(int status, InputStream stream, Map<String, List<String>> headerFields) {

                        File categoryFile = new File(dataDir, tisVersion + "/" + UpdateCenter.PLUGIN_CATEGORIES_FILENAME);
                        if (!categoryFile.exists()) {
                            try (FileOutputStream out = FileUtils.openOutputStream(categoryFile)) {
                                IOUtils.copy(stream, out);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }

                        Map<String, ExtendPluginMeta> result = Maps.newHashMap();
                        try {

                            PluginCategory category = null;
                            JSONObject body = JSONObject.parseObject(FileUtils.readFileToString(categoryFile, TisUTF8.get()));
                            JSONArray metas = null;
                            JSONObject meta = null;
                            ExtendPluginMeta pluginMeta = null;
                            for (Map.Entry<String, Object> entry : body.entrySet()) {
                                category = PluginCategory.parse(entry.getKey());
                                metas = (JSONArray) entry.getValue();

                                for (Object m : metas) {
                                    meta = (JSONObject) m;
                                    pluginMeta = new ExtendPluginMeta(meta.getString("clazz"), category);
                                    pluginMeta.descriptorClass = Optional.ofNullable(meta.getString("descClass"));
                                    pluginMeta.gav = Optional.ofNullable(meta.getString("gav"));

                                    result.put(pluginMeta.describleClass, pluginMeta);
                                }
                            }

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        return result;
                    }
                }
        );

    }

    public static void main(String[] args) {

    }

    public static class ExtendPluginMeta {
        private final String describleClass;
        private Optional<String> descriptorClass = Optional.empty();
        private Optional<String> gav = Optional.empty();
        public final PluginCategory category;

        public ExtendPluginMeta(String describleClass, PluginCategory category) {
            this.describleClass = describleClass;
            this.category = category;
        }

        public Optional<String> getGav() {
            return gav;
        }

        public Optional<String> getDescriptorClass() {
            return this.descriptorClass;
        }
    }

}
