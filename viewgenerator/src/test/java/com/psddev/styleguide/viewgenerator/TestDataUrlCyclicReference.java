package com.psddev.styleguide.viewgenerator;

import org.junit.Test;

public class TestDataUrlCyclicReference {

    @Test(expected = RuntimeException.class)
    public void testJsonDirectory() throws Exception {
        // Should throw and error
        TestUtils.getJsonDirectoryForClass(getClass()).resolveViewMaps();
    }
}
