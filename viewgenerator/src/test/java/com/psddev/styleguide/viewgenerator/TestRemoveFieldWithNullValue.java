package com.psddev.styleguide.viewgenerator;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TestRemoveFieldWithNullValue {

    @Test
    public void testJsonDirectory() throws Exception {

        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());

        generator.getContext().setExcludedPaths(Collections.singleton("base"));

        List<ViewClassDefinition> classDefs = generator.getViewClassDefinitions();

        Assert.assertEquals(1, classDefs.size());

        List<ViewClassFieldDefinition> fieldDefs = classDefs.iterator().next().getNonNullFieldDefinitions();

        Assert.assertEquals(1, fieldDefs.size());
    }
}
