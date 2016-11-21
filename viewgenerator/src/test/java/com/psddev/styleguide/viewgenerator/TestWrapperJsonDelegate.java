package com.psddev.styleguide.viewgenerator;

import org.junit.Test;

public class TestWrapperJsonDelegate {

    @Test
    public void testWrapperJsonDelegate() throws Exception {

        // get a default test view class generator
        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());

        // Just verifying that there's no errors for now.
        generator.getGeneratedClasses();
    }
}
