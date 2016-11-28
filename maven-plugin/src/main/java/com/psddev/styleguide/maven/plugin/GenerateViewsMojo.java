package com.psddev.styleguide.maven.plugin;

import java.nio.file.Path;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.psddev.styleguide.viewgenerator.ViewClassGenerator;

@Mojo(name = "generate-views", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
class GenerateViewsMojo extends AbstractStyleguideMojo {

    /**
     * The project currently being built.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        project.addCompileSourceRoot(javaSourcesOutputDirectory.getPath());

        List<Path> generateClasses = ViewClassGenerator.createInstance(
                styleguideDirectory.toPath(),
                javaSourcesOutputDirectory.toPath())
                .generateClasses();
    }
}
