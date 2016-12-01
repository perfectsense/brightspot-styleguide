package com.psddev.styleguide.codegen;

import org.junit.Test;

public class TestDataUrlCyclicReference {

    @Test(expected = ViewClassGeneratorException.class)
    public void testDataUrlCyclicReference() throws Exception {
        // Should throw and error
        TestUtils.getJsonDirectoryForClass(getClass()).resolveViewMaps();
    }
}
