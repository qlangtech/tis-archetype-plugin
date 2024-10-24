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

import java.util.List;
import java.util.Optional;

import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.maven.plugins.archetype.GenerateArchetypeMojo.ExtendPluginMeta;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-06-04 13:54
 **/
public class ExtendPlugin {
    /**
     * 可能是一个扩展点，也可能是一个具体的扩展类
     */
    public final String extend;

    private final String newClass;

    private ExtendPluginMeta pluginMeta;

    public static List<ExtendPlugin> parse(String extendPoint) {
        String[] fromExtends = StringUtils.split(extendPoint, ";");
        String[] extendPair = null;
        List<ExtendPlugin> result = Lists.newArrayList();

        for (String extend : fromExtends) {
            extendPair = StringUtils.split(extend, ":");
            if (extendPair.length != 2) {
                throw new IllegalStateException("extendPoint:" + extendPoint + " is not valid");
            }
            result.add(new ExtendPlugin(extendPair[0], extendPair[1]));
        }
        return result;
    }

    public ExtendPlugin(String extend, String newClass) {
        this.extend = extend;
        this.newClass = newClass;
    }

    public PluginClass getExtend() {
        return PluginClass.parse(this.extend);
    }

    public String getNewPackage() {
        return getExtend().getPkg() + ".extend";
    }

    public String getNewClass() {
        return newClass;
    }

    public PluginClass getNewClassInfo(boolean test) {
        String clazz = (test ? "Test" : StringUtils.EMPTY) + getNewClass();
        return new PluginClass(getNewPackage(), clazz, getNewPackage() + "." + clazz);
    }

    public ExtendPluginMeta getPluginMeta() {
        return pluginMeta;
    }

    public String getDescParentClazz() {
        Optional<String> descriptorClass = pluginMeta.getDescriptorClass();
        if (descriptorClass.isPresent()) {
            return PluginClass.parse(descriptorClass.get()).getClazzName();
        } else {
            return Descriptor.class.getSimpleName() + "<" + getExtend().getClazzName() + ">";
        }
    }

    public void setPluginMeta(ExtendPluginMeta pluginMeta) {
        this.pluginMeta = pluginMeta;
    }

    public VelocityContext createVelocityContext() {
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("plugin", this);
        //        velocityContext.put(DataxUtils.DATAX_NAME, this.dataxName);
        //        velocityContext.put("reader", reader);
        //        velocityContext.put("writer", writer);
        //        velocityContext.put("cfg", this.globalCfg);
        return velocityContext;
    }
}
