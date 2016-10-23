package com.psddev.styleguide;

import org.junit.Test;

import com.psddev.styleguide.viewgenerator.ViewClassGenerator;

import static org.junit.Assert.*;

public class TestEmptyJsonFile {

    @Test
    public void testEmptyJsonFile() throws Exception {

        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());

        // the system should skip all empty files
        // verify that there were no template definitions found
        assertEquals(0, generator.getTemplateDefinitions().get().size());
    }
}
