package com.psddev.styleguide.viewgenerator;

import java.nio.file.Paths;
import java.util.Collections;

import org.junit.Test;

public class TestStrictTypes {

    @Test
    public void testStrictTypes() throws Exception {

        // get a default test view class generator
        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());
        generator.setIgnoredFileNames(Collections.singleton(Paths.get("_config.json")));

        // generate the template definitions
        ViewClassDefinitions definitions = generator.getTemplateDefinitions();

        // Just ensures there's no error for now...

        //definitions.get().stream().map(td -> td.getJavaClassSource(true)).forEach(System.out::println);
    }
}
