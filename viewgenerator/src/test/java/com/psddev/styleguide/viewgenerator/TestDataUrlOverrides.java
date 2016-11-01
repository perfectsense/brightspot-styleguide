package com.psddev.styleguide.viewgenerator;

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
        List<ViewClassDefinition> definitions = generator.getViewClassDefinitions();

        // verify there's a definition for the "child" template
        ViewClassDefinition itemDef = definitions.stream().filter(classDef -> classDef.getViewKey().getName().equals("/templates/child")).findFirst().get();

        // get its fields
        List<ViewClassFieldDefinition> fields = itemDef.getFieldDefinitions();

        Set<String> fieldNames = fields.stream().map(ViewClassFieldDefinition::getFieldName).collect(Collectors.toSet());

        // verify that the names are foo1-5
        assertEquals(new HashSet<>(Arrays.asList("foo1", "foo2", "foo3", "foo4", "foo5")), fieldNames);
    }
}
