package com.qlangtech.tis.maven.plugins.archetype;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.codehaus.plexus.velocity.VelocityComponent;

import com.qlangtech.tis.extension.model.FormValidation;
import com.qlangtech.tis.extension.model.UpdateSite;
import com.qlangtech.tis.manage.common.Config;

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

    @Parameter(property = "tis.version", required = true)
    private String tisVersion;

    @Parameter(property = "tis.extendpoint", required = true)
    private String extendPoint;

    @Component
    private VelocityComponent velocity;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        System.setProperty(Config.KEY_JAVA_RUNTIME_PROP_ENV_PROPS, "true");
        Config.setDataDir(".");
        //System.setProperty(Config.KEY_DATA_DIR, ".");
        //  Objects.requireNonNull(velocity, "velocity can not be null");
        // this.getLog().info(velocity.getClass().getName());
        // this.getLog().info("tisVersion:" + tisVersion);
        UpdateSite updateSite = UpdateSite.tisDftUpdateSite(tisVersion);
        // this.tisVersion;
        try {
            Future<FormValidation> f = updateSite.updateDirectly();
            f.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // updateSite.getData()

    }
}
