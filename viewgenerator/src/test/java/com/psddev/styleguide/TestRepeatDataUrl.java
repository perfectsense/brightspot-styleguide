package com.psddev.styleguide;

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
        TemplateDefinitions definitions = generator.getTemplateDefinitions();

        // verify there's a definition for the list template
        TemplateDefinition itemDef = definitions.getByName("templates/list");

        // get its fields
        List<TemplateFieldDefinition> fields = itemDef.getFields(definitions);

        // find a field named "items"
        TemplateFieldDefinition itemsFieldDef = fields.stream().filter(field -> "items".equals(field.getName())).findFirst().get();

        // verify it's a list
        assertTrue(itemsFieldDef instanceof TemplateFieldDefinitionList);

        TemplateFieldDefinitionList itemsListFieldDef = (TemplateFieldDefinitionList) itemsFieldDef;

        // get the list item types
        Set<String> listItemTypes = itemsListFieldDef.getListItemTypes();

        // verify there's only 1 type
        assertEquals(1, listItemTypes.size());

        // verify the type is "item"
        assertEquals("/templates/item", listItemTypes.stream().findFirst().get());
    }
}
