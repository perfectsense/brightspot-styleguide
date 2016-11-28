package com.psddev.styleguide.maven.plugin;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

abstract class AbstractStyleguideMojo extends AbstractMojo {

    /**
     * The directory containing all of the FE styleguide resources.
     */
    @Parameter(
            property = "styleguideDirectory",
            defaultValue = "${project.build.directory}" + "/" + "${project.build.finalName}")
    protected File styleguideDirectory;

    /**
     * The directory where the generated java source files should be placed.
     */
    @Parameter(
            property = "javaSourcesOutputDirectory",
            defaultValue = "${project.build.directory}" + "/" + "generated-sources/styleguide")
    protected File javaSourcesOutputDirectory;
}
