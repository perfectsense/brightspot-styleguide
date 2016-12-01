package com.psddev.styleguide.codegen;

import org.junit.Test;

public class TestDataUrlComplex {

    @Test
    public void testDataUrlComplex() throws Exception {
        // Should throw and error
        TestUtils.getJsonDirectoryForClass(getClass()).resolveViewMaps();
    }
}
