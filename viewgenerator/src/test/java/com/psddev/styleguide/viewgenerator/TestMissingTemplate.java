package com.psddev.styleguide.viewgenerator;

import org.junit.Test;

public class TestMissingTemplate {

    @Test(expected = ViewClassGeneratorException.class)
    public void testMissingTemplate() throws Exception {
        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());

        // should throw an exception for having a data file without a _template or _view field
        generator.getGeneratedClasses();
    }
}
