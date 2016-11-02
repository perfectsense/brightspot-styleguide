package com.psddev.styleguide.viewgenerator;

import org.junit.Test;

public class TestMultipleEffectiveTypesError {

    @Test(expected = RuntimeException.class)
    public void testMultipleEffectiveTypesError() throws Exception {

        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());

        generator.getGeneratedClasses();
    }
}
