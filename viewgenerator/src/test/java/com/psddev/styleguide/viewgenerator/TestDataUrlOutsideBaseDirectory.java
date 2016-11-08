package com.psddev.styleguide.viewgenerator;

import java.nio.file.Path;

import org.junit.Test;

public class TestDataUrlOutsideBaseDirectory {

    @Test(expected = RuntimeException.class)
    public void testJsonDirectory() throws Exception {

        Path jsonPath = TestUtils.getJsonDirectoriesForClasses(getClass()).iterator().next().resolve("base");
        JsonDirectory jsonDir = new JsonDirectory(new ViewClassGeneratorContext(), jsonPath);

        jsonDir.resolveViewMaps();
    }
}
