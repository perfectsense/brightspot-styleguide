package com.psddev.styleguide;

import org.junit.Test;

public class TestMissingTemplate {

    @Test(expected = MissingTemplateException.class)
    public void testMissingTemplate() throws Exception {
        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());

        // should throw an exception for having a data file without a _template field
        generator.getGeneratedClasses();
    }
}
