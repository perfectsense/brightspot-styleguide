package com.psddev.styleguide.viewgenerator;

import java.util.List;

import org.junit.Test;

public class TestStrictTypes {

    @Test
    public void testStrictTypes() throws Exception {

        // get a default test view class generator
        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());

        // generate the template definitions
        List<ViewClassDefinition> definitions = generator.getViewClassDefinitions();

        // Just ensures there's no error for now...

        //definitions.get().stream().map(td -> td.getJavaClassSource(true)).forEach(System.out::println);
    }
}
