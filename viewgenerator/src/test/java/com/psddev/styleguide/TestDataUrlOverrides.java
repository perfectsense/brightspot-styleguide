package com.psddev.styleguide;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestDataUrlOverrides {

    @Test
    public void testDataUrlOverrides() throws Exception {

        // get a default test view class generator
        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());

        // generate the template definitions
        TemplateDefinitions definitions = generator.getTemplateDefinitions();

        // verify there's a definition for the "child" template
        TemplateDefinition itemDef = definitions.getByName("templates/child");

        // get its fields
        List<TemplateFieldDefinition> fields = itemDef.getFields();

        Set<String> fieldNames = fields.stream().map(TemplateFieldDefinition::getName).collect(Collectors.toSet());

        // verify that the names are foo1-5
        assertEquals(new HashSet<>(Arrays.asList("foo1", "foo2", "foo3", "foo4", "foo5")), fieldNames);
    }
}
