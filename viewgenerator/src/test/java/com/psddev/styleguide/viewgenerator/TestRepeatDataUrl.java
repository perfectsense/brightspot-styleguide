package com.psddev.styleguide.viewgenerator;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestRepeatDataUrl {

    @Test
    public void testRepeatDataUrl() throws Exception {
        // get a default test view class generator
        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());

        // generate the template definitions
        List<ViewClassDefinition> definitions = generator.getViewClassDefinitions();

        // verify there's a definition for the list template
        ViewClassDefinition itemDef = definitions.stream().filter(classDef -> classDef.getViewKey().getName().equals("/templates/list")).findFirst().get();

        // get its fields
        List<ViewClassFieldDefinition> fields = itemDef.getFieldDefinitions();

        // find a field named "items"
        ViewClassFieldDefinition itemsFieldDef = fields.stream().filter(field -> "items".equals(field.getFieldName())).findFirst().get();

        // verify it's a list
        assertTrue(itemsFieldDef.getEffectiveType() == JsonList.class);

        // get the list item types
        Set<ViewClassFieldType> listItemTypes = itemsFieldDef.getFieldValueTypes();

        // verify there's only 1 type
        assertEquals(1, listItemTypes.size());

        // verify the type is "item"
        assertEquals("templates.Item", listItemTypes.stream().findFirst().get().getFullyQualifiedClassName());
    }
}
