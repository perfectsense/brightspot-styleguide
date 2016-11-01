package com.psddev.styleguide.viewgenerator;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestEmptyJsonFile {

    @Test(expected = RuntimeException.class)
    public void testEmptyJsonFile() throws Exception {

        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());

        // the system should skip all empty files
        // verify that there were no template definitions found
        assertEquals(0, generator.getViewClassDefinitions().size());
    }
}
