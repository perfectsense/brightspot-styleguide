package com.psddev.styleguide.viewgenerator;

import org.junit.Test;

import com.psddev.styleguide.*;
import com.psddev.styleguide.viewgenerator.ViewClassGenerator;

public class TestMissingTemplate {

    @Test(expected = MissingTemplateException.class)
    public void testMissingTemplate() throws Exception {
        ViewClassGenerator generator = com.psddev.styleguide.TestUtils.getDefaultGeneratorForClass(getClass());

        // should throw an exception for having a data file without a _template or _view field
        generator.getGeneratedClasses();
    }
}
