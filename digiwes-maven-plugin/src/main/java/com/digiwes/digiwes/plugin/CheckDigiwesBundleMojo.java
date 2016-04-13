package com.digiwes.digiwes.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


/**
 * Created by dongwh on 2016-3-30.
 *
 * Goal which touches a timestamp file.
 */
@Mojo(name = "digiwes", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class CheckDigiwesBundleMojo extends AbstractMojo {


    @Parameter(alias = "something", property = "something", defaultValue = "something default value")
    private String something;

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("custom maven plugin start##################################################");
        getLog().info("something=============================================================="+something);
    }

}
