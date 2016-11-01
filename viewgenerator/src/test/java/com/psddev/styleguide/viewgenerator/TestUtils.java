package com.psddev.styleguide.viewgenerator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestUtils {

    private static final String TEST_RESOURCES_DIRECTORY = "src/test/resources";

    private static final String DEFAULT_JAVA_PACKAGE_NAME = "com.psddev.styleguide.view";

    public static JsonDirectory getJsonDirectoryForName(String name) {
        Path path = getJsonDirectoriesForNames(name).stream().findFirst().orElse(null);
        ViewClassGeneratorContext context = new ViewClassGeneratorContext();
        return new JsonDirectory(context, path);
    }

    public static JsonDirectory getJsonDirectoryForClass(Class<?> klass) {
        Path path = getJsonDirectoriesForClasses(klass).stream().findFirst().orElse(null);
        ViewClassGeneratorContext context = new ViewClassGeneratorContext();
        return new JsonDirectory(context, path);
    }

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

        Set<Path> jsonDirectories = getJsonDirectoriesForClasses(klass);
        String javaPackageName = DEFAULT_JAVA_PACKAGE_NAME;

        ViewClassGeneratorContext context = new ViewClassGeneratorContext();
        context.setJsonDirectory(jsonDirectories.iterator().next());

        context.setJavaSourceDirectory(jsonDirectories.stream().findFirst().orElse(null).resolve("output"));
        context.setDefaultJavaPackagePrefix(javaPackageName);

        ViewClassGenerator generator = new ViewClassGenerator(context);

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
