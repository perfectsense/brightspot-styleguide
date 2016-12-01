package com.psddev.styleguide.codegen;

import java.nio.file.Path;
import java.util.Collections;

import org.junit.Test;

public class TestDataUrlOutsideBaseDirectory {

    @Test(expected = RuntimeException.class)
    public void testDataUrlOutsideBaseDirectory() throws Exception {

        Path jsonPath = TestUtils.getJsonDirectoryPathForClass(getClass()).resolve("base");

        ViewClassGeneratorContext context = new ViewClassGeneratorContext();
        context.setJsonDirectories(Collections.singleton(jsonPath));

        JsonDirectory jsonDir = new JsonDirectory(context);

        jsonDir.resolveViewMaps();
    }
}
