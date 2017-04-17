package com.psddev.styleguide.codegen;

import org.junit.Test;

public class TestDataUrlListReference {

    @Test
    public void testDataUrlListReference() {
        // Should not throw an error
        TestUtils.getDefaultGeneratorForClass(getClass()).getGeneratedClasses();
    }
}
