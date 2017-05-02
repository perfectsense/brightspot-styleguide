package com.psddev.styleguide.codegen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestArrayJsonFile {

    @Test
    public void testArrayJsonFile() throws Exception {

        JsonDirectory directory = TestUtils.getJsonDirectoryForClass(getClass());

        Set<JsonViewMap> viewMaps = directory.resolveViewMaps();
        Assert.assertEquals(11, viewMaps.size());

        List<ViewClassDefinition> definitions = ViewClassDefinition.createDefinitions(directory.getContext(), viewMaps);
        assertEquals(5, definitions.size());

        Map<String, ViewClassDefinition> definitionsMap = new HashMap<>();
        definitions.forEach(def -> definitionsMap.put(def.getViewKey().getName(), def));

        ViewClassDefinition containerDef = definitionsMap.get("Container.hbs");
        assertEquals(2, containerDef.getFieldDefinitions().size());
        assertEquals(4, containerDef.getFieldDefinitions().stream()
                .filter(fieldDef -> "modules".equals(fieldDef.getFieldName()))
                .findFirst()
                .orElse(null)
                .getFieldValueTypes().size());

        ViewClassDefinition module1Def = definitionsMap.get("Module1.hbs");
        assertEquals(1, module1Def.getFieldDefinitions().size());
        assertEquals("title1", module1Def.getFieldDefinitions().get(0).getFieldName());

        ViewClassDefinition module2Def = definitionsMap.get("Module2.hbs");
        assertEquals(1, module1Def.getFieldDefinitions().size());
        assertEquals("title2", module2Def.getFieldDefinitions().get(0).getFieldName());

        ViewClassDefinition module3Def = definitionsMap.get("Module3.hbs");
        assertEquals(1, module1Def.getFieldDefinitions().size());
        assertEquals("title3", module3Def.getFieldDefinitions().get(0).getFieldName());

        ViewClassDefinition module4Def = definitionsMap.get("Module4.hbs");
        assertEquals(1, module1Def.getFieldDefinitions().size());
        assertEquals("title4", module4Def.getFieldDefinitions().get(0).getFieldName());
    }
}
