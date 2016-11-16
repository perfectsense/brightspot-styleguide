package com.psddev.styleguide.viewgenerator;

import org.junit.Test;

public class TestMultipleEffectiveTypesError {

    @Test(expected = ViewClassGeneratorException.class)
    public void testMultipleEffectiveTypesError() throws Exception {

        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());

        generator.getGeneratedClasses();
    }
}
