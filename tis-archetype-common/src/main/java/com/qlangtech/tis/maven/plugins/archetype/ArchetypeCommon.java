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
import java.io.IOException;

import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.Config.SysDBType;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import org.apache.commons.io.FileUtils;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-06-20 11:29
 **/
public class ArchetypeCommon {

    public static final String UBER_DATA_DIR = "uber-data";

    public static void initSysParams() {
        System.setProperty(Config.KEY_JAVA_RUNTIME_PROP_ENV_PROPS, "true");
        // System.setProperty(Config.KEY_ZK_HOST, StringUtils.EMPTY);
        System.setProperty(Config.KEY_ASSEMBLE_HOST, Config.KEY_ASSEMBLE_HOST);
        System.setProperty(Config.KEY_RUNTIME, RunEnvironment.DAILY.getKeyName());
        System.setProperty(Config.KEY_TIS_HOST, Config.KEY_TIS_HOST);
        System.setProperty(Config.KEY_TIS_DATASOURCE_TYPE, SysDBType.DERBY.getToken());
        System.setProperty(Config.KEY_TIS_DATASOURCE_DBNAME, "tis_console_db");
    }

    public static File getTmpDir() {
        File tmpDir = null;
        try {
            tmpDir = (new File(FileUtils.getUserDirectory(), "tis_tmp"));
            // tmpDir = ;
            File tmpDataDir = new File(tmpDir, UBER_DATA_DIR + "/data");
            FileUtils.forceMkdir(tmpDataDir);
            Config.setDataDir(tmpDataDir.getAbsolutePath());
            //  FileUtils.forceMkdir(new File(Config.getLibDir(), com.qlangtech.tis.TIS.KEY_TIS_PLUGIN_ROOT));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return tmpDir;
    }
}
