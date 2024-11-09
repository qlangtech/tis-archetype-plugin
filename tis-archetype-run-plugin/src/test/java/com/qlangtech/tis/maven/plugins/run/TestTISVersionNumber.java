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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2024-11-02 10:48
 **/
public class TestTISVersionNumber {
    @Test
    public void testCompare() {
        // 仓库中的文件
        TISVersionNumber srcV = new TISVersionNumber("3.1.0-SNAPSHOT", 2l);
        // 本地文件
        TISVersionNumber destV = new TISVersionNumber("3.1.0-SNAPSHOT", 1l);

        if (srcV.compareTo(destV) <= 0) {
            // 说明本地依然最新，不需要更新
            Assert.fail("shall note execute to this fork");
        }
    }
}
