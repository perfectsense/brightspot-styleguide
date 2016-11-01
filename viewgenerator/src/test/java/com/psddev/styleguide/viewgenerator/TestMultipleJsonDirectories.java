package com.psddev.styleguide.viewgenerator;

import java.nio.file.Path;

import org.junit.Test;

public class TestMultipleJsonDirectories {

    @Test
    public void testDataUrlOverrides() throws Exception {

        /*
            <argument>--json-dir=${project.basedir}/bower_components/brightspot-base/styleguide</argument>
            <argument>--json-dir=${project.basedir}/styleguide</argument>
            <argument>--java-package-prefix=com.inspireconfidence.view</argument>
            <argument>--build-dir=${project.build.directory}/generated-sources/styleguide</argument>
            <argument>--ignore-files=_config.json</argument>
            <argument>--watch=${styleguide.viewgenerator.watch}</argument>
         */

        Path testPath = TestUtils.getJsonDirectoriesForClasses(getClass()).iterator().next();

        // need to append _test to directory name so git doesn't ignore it.
        Path bowerComponentsPath = testPath.resolve("bower_components_test/brightspot-base/styleguide");
        Path styleguidePath = testPath.resolve("styleguide");

        String javaPackageName = "com.inspireconfidence.view";

        String[] args = {
                "--json-dir=" + bowerComponentsPath.toRealPath().toString(),
                "--json-dir=" + styleguidePath.toRealPath().toString(),
                "--java-package-prefix=" + javaPackageName,
                "--ignore-files=" + "_config.json"
        };

        ViewClassGeneratorCliArguments cliArgs = new ViewClassGeneratorCliArguments(args);

        ViewClassGenerator generator = new ViewClassGenerator(cliArgs);

        generator.disableLogColors();

        generator.getGeneratedClasses();
    }
}
