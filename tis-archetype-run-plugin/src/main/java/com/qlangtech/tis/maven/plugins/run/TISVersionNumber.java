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

import hudson.util.VersionNumber;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2024-11-02 10:40
 **/
public class TISVersionNumber extends VersionNumber {
    private final long lastModify;

    public TISVersionNumber(String version, long lastModify) {
        super(version);
        this.lastModify = lastModify;
    }

    @Override
    public int compareTo(VersionNumber o) {
        int val = super.compareTo(o);
        if (val == 0 && o instanceof TISVersionNumber) {
            return (int) (this.lastModify - ((TISVersionNumber) o).lastModify);
        }
        return val;
    }
}
