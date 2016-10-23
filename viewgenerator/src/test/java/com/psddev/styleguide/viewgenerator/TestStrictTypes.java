package com.psddev.styleguide.viewgenerator;

import java.nio.file.Paths;
import java.util.Collections;

import org.junit.Test;

import com.psddev.styleguide.*;
import com.psddev.styleguide.viewgenerator.TemplateDefinitions;
import com.psddev.styleguide.viewgenerator.ViewClassGenerator;

public class TestStrictTypes {

    @Test
    public void testStrictTypes() throws Exception {

        // get a default test view class generator
        ViewClassGenerator generator = com.psddev.styleguide.TestUtils.getDefaultGeneratorForClass(getClass());
        generator.setIgnoredFileNames(Collections.singleton(Paths.get("_config.json")));

        // generate the template definitions
        TemplateDefinitions definitions = generator.getTemplateDefinitions();

        // Just ensures there's no error for now...

        //definitions.get().stream().map(td -> td.getJavaClassSource(true)).forEach(System.out::println);
    }
}
