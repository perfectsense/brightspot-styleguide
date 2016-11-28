package com.psddev.styleguide.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import com.psddev.styleguide.viewgenerator.ViewClassGenerator;

@Mojo(name = "watch", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
class WatchMojo extends AbstractStyleguideMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ViewClassGenerator.createInstance(
                styleguideDirectory.toPath(),
                javaSourcesOutputDirectory.toPath())
                .watch();
    }
}
