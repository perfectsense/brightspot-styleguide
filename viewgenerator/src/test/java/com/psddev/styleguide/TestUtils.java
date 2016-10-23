package com.psddev.styleguide;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.psddev.styleguide.viewgenerator.ViewClassGenerator;

public class TestUtils {

    private static final String TEST_RESOURCES_DIRECTORY = "src/test/resources";

    private static final String DEFAULT_JAVA_PACKAGE_NAME = "com.psddev.styleguide.view";

    public static Set<Path> getJsonDirectoriesForNames(String... names) {
        return Stream.of(names)
                .map(name -> getTestResourcesPath().resolve(name))
                .collect(Collectors.toSet());
    }

    public static Set<Path> getJsonDirectoriesForClasses(Class<?>... classes) {
        return Stream.of(classes)
                .map(klass -> getTestResourcesPath().resolve(klass.getSimpleName()))
                .collect(Collectors.toSet());
    }

    public static ViewClassGenerator getDefaultGeneratorForClass(Class<?> klass) {

        Set<Path> jsonDirectories = TestUtils.getJsonDirectoriesForClasses(klass);
        String javaPackageName = DEFAULT_JAVA_PACKAGE_NAME;

        ViewClassGenerator generator = new ViewClassGenerator(
                jsonDirectories,
                jsonDirectories.stream().findFirst().orElse(null).resolve("output"),
                javaPackageName);

        generator.disableLogColors();

        return generator;
    }

    private static Path getTestResourcesPath() {

        Path modulePath;

        Path userDir = Paths.get(System.getProperty("user.dir"));

        if (!userDir.endsWith("viewgenerator")) {
            modulePath = userDir.resolve("viewgenerator");
        } else {
            modulePath = userDir;
        }

        return modulePath.resolve(TEST_RESOURCES_DIRECTORY);
    }
}
