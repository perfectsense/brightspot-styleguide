package com.psddev.styleguide.codegen;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestEmptyJsonFile {

    @Test(expected = ViewClassGeneratorException.class)
    public void testEmptyJsonFile() throws Exception {

        List<ViewClassDefinition> definitions = TestUtils.getViewClassDefinitionsForClass(getClass());

        // the system should skip all empty files
        // verify that there were no template definitions found
        assertEquals(0, definitions.size());
    }
}
