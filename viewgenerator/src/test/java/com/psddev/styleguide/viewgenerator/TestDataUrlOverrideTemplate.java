package com.psddev.styleguide.viewgenerator;

import org.junit.Test;

import com.psddev.styleguide.*;
import com.psddev.styleguide.viewgenerator.ViewClassGenerator;

public class TestDataUrlOverrideTemplate {

    @Test(expected = DataUrlTemplateOverrideException.class)
    public void testDataUrlOverrides() throws Exception {

        // get a default test view class generator
        ViewClassGenerator generator = com.psddev.styleguide.TestUtils.getDefaultGeneratorForClass(getClass());

        // generate the template definitions
        generator.getTemplateDefinitions();
    }
}
