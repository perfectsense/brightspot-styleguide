package com.psddev.styleguide.codegen;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class TestUtils {

    static final String TEST_RESOURCES_DIRECTORY = "src/test/resources";

    public static Path getJsonDirectoryPathForClass(Class<?> klass) {
        return getTestResourcesPath().resolve(klass.getSimpleName());
    }

    public static ViewClassGeneratorContext getViewClassGeneratorContextForClass(Class<?> klass) {

        Path jsonDirectoryPath = getJsonDirectoryPathForClass(klass);

        ViewClassGeneratorContext context = new ViewClassGeneratorContext();
        context.setJsonDirectories(Collections.singleton(jsonDirectoryPath));
        context.setJavaSourceDirectory(jsonDirectoryPath.resolve("output"));

        return context;
    }

    public static JsonDirectory getJsonDirectoryForClass(Class<?> klass) {
        return new JsonDirectory(getViewClassGeneratorContextForClass(klass));
    }

    public static List<ViewClassDefinition> getViewClassDefinitionsForClass(Class<?> klass) {
        JsonDirectory directory = getJsonDirectoryForClass(klass);
        return ViewClassDefinition.createDefinitions(directory.getContext(), directory.resolveViewMaps());
    }

    public static ViewClassGenerator getDefaultGeneratorForClass(Class<?> klass) {
        ViewClassGenerator generator = new ViewClassGenerator(getViewClassGeneratorContextForClass(klass));

        generator.disableLogColors();

        return generator;
    }

    static Path getTestResourcesPath() {

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
