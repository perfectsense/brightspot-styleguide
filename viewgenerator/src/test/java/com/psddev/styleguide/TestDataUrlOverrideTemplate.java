package com.psddev.styleguide;

import org.junit.Test;

import com.psddev.styleguide.viewgenerator.ViewClassGenerator;

public class TestDataUrlOverrideTemplate {

    @Test(expected = DataUrlTemplateOverrideException.class)
    public void testDataUrlOverrides() throws Exception {

        // get a default test view class generator
        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());

        // generate the template definitions
        generator.getTemplateDefinitions();
    }
}
