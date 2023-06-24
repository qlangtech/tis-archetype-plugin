package com.qlangtech.tis.maven.plugins.archetype;

import org.apache.commons.lang.StringUtils;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-06-06 16:38
 **/
public class PluginClass {
    private final String clazzName;
    private final String pkg;
    private final String fullClazz;

    public PluginClass(String pkg, String clazzName, String fullClazz) {
        this.pkg = pkg;
        int indexOfDoller;
        if ((indexOfDoller = StringUtils.indexOf(clazzName, "$")) > -1) {
            this.clazzName = StringUtils.substring(clazzName, indexOfDoller + 1);
            this.fullClazz = StringUtils.replace(fullClazz, "$", ".");
        } else {
            this.clazzName = clazzName;
            this.fullClazz = fullClazz;
        }

    }

    public static PluginClass parse(String extend) {
        return new PluginClass(StringUtils.substringBeforeLast(extend, ".")
                , StringUtils.substringAfterLast(extend, "."), extend);
    }

    public String getPkg() {
        return this.pkg;
    }

    public String getClazzName() {
        return this.clazzName;
    }

    public String getFullClazz() {
        return this.fullClazz;
    }

    public String getFullClazzRelativePath() {
        return getFullRelativePath("java");
    }

    public String getFullRelativePath(String fileExtend) {
        return StringUtils.replace(this.fullClazz, ".", "/") + "." + fileExtend;
    }
}
