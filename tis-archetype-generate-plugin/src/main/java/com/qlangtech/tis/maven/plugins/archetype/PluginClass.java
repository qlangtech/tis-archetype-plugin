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
        return new PluginClass(
                StringUtils.substringBeforeLast(extend, "."), StringUtils.substringAfterLast(extend, "."), extend);
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
