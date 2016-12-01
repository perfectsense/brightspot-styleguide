package com.psddev.styleguide.codegen;

import java.util.List;

import org.junit.Test;

public class TestStrictTypes {

    @Test
    public void testStrictTypes() throws Exception {

        // generate the template definitions
        List<ViewClassDefinition> definitions = TestUtils.getViewClassDefinitionsForClass(getClass());

        // Just ensures there's no error for now...

        //definitions.get().stream().map(td -> td.getJavaClassSource(true)).forEach(System.out::println);
    }
}
