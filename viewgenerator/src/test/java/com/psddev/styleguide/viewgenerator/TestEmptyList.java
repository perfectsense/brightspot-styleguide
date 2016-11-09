package com.psddev.styleguide.viewgenerator;

import org.junit.Test;

public class TestEmptyList {

    @Test(expected = RuntimeException.class)
    public void testEmptyList() throws Exception {
        // Should throw an error
        TestUtils.getDefaultGeneratorForClass(getClass()).getGeneratedClasses();
    }
}
