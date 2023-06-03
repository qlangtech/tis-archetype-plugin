package com.qlangtech.tis.maven.plugins.archetype;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.Objects;

import org.codehaus.plexus.velocity.VelocityComponent;

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

    @Component
    private VelocityComponent velocity;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Objects.requireNonNull(velocity, "velocity can not be null");
        this.getLog().info(velocity.getClass().getName());
    }
}
