package com.psddev.styleguide.viewgenerator;

import org.junit.Test;

public class TestDataUrlOverrideTemplate {

    @Test(expected = RuntimeException.class)
    public void testDataUrlOverrides() throws Exception {

        // get a default test view class generator
        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());

        // generate the template definitions
        generator.getViewClassDefinitions();
    }
}
