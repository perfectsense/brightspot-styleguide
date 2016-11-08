package com.psddev.styleguide.viewgenerator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;

public class TestFieldNames {

    /*
     * For each key in the Main.json file, it has a value of either true or
     * false. If it is true, then it should be a valid field name. If it is
     * false, then it should NOT be a valid field name. This test uses the
     * values to validate that the view generator correctly determines whether
     * a field name is valid or not
     */
    @Test
    public void testFieldNames() throws Exception {

        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());

        List<ViewClassDefinition> classDefs = generator.getViewClassDefinitions();

        // makes sure there's only one class definition
        Assert.assertEquals(1, classDefs.size());

        ViewClassDefinition classDef = classDefs.get(0);

        List<ViewClassDefinitionError> errors = classDef.getErrors();

        // put the errors in a map keyed off of field name.
        Map<String, ViewClassFieldDefinition> errorFieldDefs = new LinkedHashMap<>();
        for (ViewClassDefinitionError error : errors) {
            errorFieldDefs.put(error.getFieldDefinition().getFieldName(), error.getFieldDefinition());
        }

        // Collect the field names and their expected values in a map
        Map<String, Boolean> fieldNames = new LinkedHashMap<>();

        Path mainJsonPath = TestUtils.getTestResourcesPath().resolve(getClass().getSimpleName()).resolve("Main.json");

        @SuppressWarnings("unchecked")
        Map<String, Object> mainJsonMap = (Map<String, Object>) ObjectUtils.fromJson(IoUtils.toString(mainJsonPath.toFile(), StandardCharsets.UTF_8));

        for (Map.Entry<String, Object> entry : mainJsonMap.entrySet())  {

            String key = entry.getKey();

            if ("_template".equals(key)) {
                continue;
            }

            fieldNames.put(key, (Boolean) entry.getValue());
        }

        // validate that each field's expected value matches the actual.
        for (Map.Entry<String, Boolean> entry : fieldNames.entrySet())  {
            Assert.assertEquals(entry.getValue()
                    ? "Expected field [" + entry.getKey() + "] to not generate an error but it did."
                    : "Expected field [" + entry.getKey() + "] to generate an error but it did not.",
                    entry.getValue(),
                    !errorFieldDefs.containsKey(entry.getKey()));
        }
    }
}
