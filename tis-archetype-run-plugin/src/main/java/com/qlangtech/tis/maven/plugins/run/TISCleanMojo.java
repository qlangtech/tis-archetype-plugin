package com.qlangtech.tis.maven.plugins.run;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;

import com.qlangtech.tis.maven.plugins.archetype.ArchetypeCommon;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-06-30 13:58
 **/
@Mojo(name = "clean", requiresProject = true)
public class TISCleanMojo extends AbstractMojo {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ArchetypeCommon.initSysParams();
        File tmpDir = ArchetypeCommon.getTmpDir();
        try {
            FileUtils.forceDelete(tmpDir);
            this.getLog().info("tmpDir has bean removed:" + tmpDir.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }
}
