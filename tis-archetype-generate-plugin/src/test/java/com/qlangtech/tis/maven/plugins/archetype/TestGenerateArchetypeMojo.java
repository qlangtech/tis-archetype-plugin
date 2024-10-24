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

import java.io.File;
import java.util.Properties;

import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.manage.common.TisUTF8;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.app.VelocityEngine;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-06-05 18:21
 **/
public class TestGenerateArchetypeMojo {
    static GenerateArchetypeMojo mojo;

    @BeforeClass
    public static void initialize() {
        mojo = new GenerateArchetypeMojo();
        mojo.tisVersion = "3.7.2";

        initVelocityEngine(mojo);
    }

    @After
    public void afterTest() {
        ArchetypeProject projectModel = mojo.getProjectModel();
        FileUtils.deleteQuietly(projectModel.getDistDir());
    }

    @Test
    public void testGenerateDatasourceFactory() throws Exception {

        mojo.artifactId = "tis-db2-plugin";
        mojo.extendPoint = "com.qlangtech.tis.plugin.ds.BasicDataSourceFactory:DB2DataSourceFactory";

        mojo.execute();

        ArchetypeProject projectModel = mojo.getProjectModel();
        File pom = new File(projectModel.getDistDir(), "pom.xml");

        Assert.assertEquals(
                StringUtils.trimToEmpty(IOUtils.loadResourceFromClasspath(
                        TestGenerateArchetypeMojo.class, "generateDatasourceFactory_pom.xml")),
                StringUtils.trimToEmpty(FileUtils.readFileToString(pom, TisUTF8.get())));
    }

    /**
     * reader
     */
    @Test
    public void testGenerateDataXReader() throws Exception {

        mojo.artifactId = "tis-db2-plugin";
        mojo.extendPoint = "com.qlangtech.tis.datax.impl.DataxReader:DataXDB2Reader";

        try {
            mojo.execute();
            Assert.fail("shall throw Exception of artifactId:tis-db2-plugin is invalid");
        } catch (IllegalStateException e) {
            Assert.assertEquals(
                    "artifactId:" + mojo.artifactId + " is invalid, shall be pattern of:"
                            + ArchetypeProject.PATTERN_BATCH_NAME,
                    e.getMessage());
        }

        mojo.artifactId = "tis-datax-db2-plugin";
        mojo.execute();
    }

    /**
     * writer
     */
    @Test
    public void testGenerateDataXWriter() throws Exception {

        mojo.artifactId = "tis-db2-plugin";
        mojo.extendPoint =
                "com.qlangtech.tis.datax.impl.DataxWriter:DataXDB2Writer;com.qlangtech.tis.datax.impl.DataxReader:DataXDB2Reader";

        try {
            mojo.execute();
            Assert.fail("shall throw Exception of artifactId:tis-db2-plugin is invalid");
        } catch (IllegalStateException e) {
            Assert.assertEquals(
                    "artifactId:" + mojo.artifactId + " is invalid, shall be pattern of:"
                            + ArchetypeProject.PATTERN_BATCH_NAME,
                    e.getMessage());
        }

        mojo.artifactId = "tis-datax-db2-plugin";
        mojo.execute();
    }

    @Test
    public void testGenerateIncr() throws Exception {

        mojo.artifactId = "tis-db2-plugin";
        mojo.extendPoint =
                "com.qlangtech.tis.plugin.incr.TISSinkFactory:DB2SinkFactory;com.qlangtech.tis.async.message.client.consumer.impl.MQListenerFactory:FlinkCDCDB2SourceFactory";

        try {
            mojo.execute();
            Assert.fail("shall throw Exception of artifactId:" + mojo.artifactId + " is invalid");
        } catch (IllegalStateException e) {
            Assert.assertEquals(
                    "artifactId:" + mojo.artifactId + " is invalid, shall be pattern of:"
                            + ArchetypeProject.PATTERN_INCR_NAME,
                    e.getMessage());
        }

        mojo.artifactId = "tis-flink-db2-plugin";
        mojo.execute();
    }

    /**
     * 测试 将
     */
    @Test
    public void testFaildMixIncrBatch() throws Exception {

        mojo.artifactId = "tis-db2-plugin";
        mojo.extendPoint =
                "com.qlangtech.tis.plugin.incr.TISSinkFactory:DB2SinkFactory;com.qlangtech.tis.datax.impl.DataxWriter:DataXDB2Writer";

        try {
            mojo.execute();
            Assert.fail("extned batch and incr can not mix in one plugin");
        } catch (IllegalStateException e) {
            Assert.assertEquals("extned batch and incr can not mix in one plugin", e.getMessage());
        }

        //        mojo.artifactId = "tis-flink-db2-plugin";
        //        mojo.execute();

    }

    private static void initVelocityEngine(GenerateArchetypeMojo mojo) {
        VelocityEngine velocityEngine = new VelocityEngine();
        Properties prop = new Properties();
        prop.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");
        prop.setProperty("resource.loader", "tisLoader");
        prop.setProperty("tisLoader.resource.loader.class", TestClasspathResourceLoader.class.getName());
        velocityEngine.init(prop);

        mojo.velocity = () -> {
            return velocityEngine;
        };
    }
}
