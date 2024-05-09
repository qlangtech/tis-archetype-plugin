package com.qlangtech.tis.maven.plugins.archetype;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.pubhook.common.RunEnvironment;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-06-20 11:29
 **/
public class ArchetypeCommon {

    public static final String UBER_DATA_DIR = "uber-data";

    public static void initSysParams() {
        System.setProperty(Config.KEY_JAVA_RUNTIME_PROP_ENV_PROPS, "true");
        //System.setProperty(Config.KEY_ZK_HOST, StringUtils.EMPTY);
        System.setProperty(Config.KEY_ASSEMBLE_HOST, Config.KEY_ASSEMBLE_HOST);
        System.setProperty(Config.KEY_RUNTIME, RunEnvironment.DAILY.getKeyName());
        System.setProperty(Config.KEY_TIS_HOST, Config.KEY_TIS_HOST);
        System.setProperty(Config.KEY_TIS_DATASOURCE_TYPE, Config.DB_TYPE_DERBY);
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
