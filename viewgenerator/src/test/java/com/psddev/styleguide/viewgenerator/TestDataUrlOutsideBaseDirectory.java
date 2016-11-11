package com.psddev.styleguide.viewgenerator;

import java.nio.file.Path;
import java.util.Collections;

import org.junit.Test;

public class TestDataUrlOutsideBaseDirectory {

    @Test(expected = RuntimeException.class)
    public void testJsonDirectory() throws Exception {

        Path jsonPath = TestUtils.getJsonDirectoriesForClasses(getClass()).iterator().next().resolve("base");

        ViewClassGeneratorContext context = new ViewClassGeneratorContext();
        context.setJsonDirectories(Collections.singleton(jsonPath));

        JsonDirectory jsonDir = new JsonDirectory(context);

        jsonDir.resolveViewMaps();
    }
}
