package com.psddev.styleguide.viewgenerator;

import org.junit.Test;

public class TestPackageJsonExistence {

    @Test
    public void testPackageJsonExistence() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        jsonDir.resolveViewMaps();
    }
}
