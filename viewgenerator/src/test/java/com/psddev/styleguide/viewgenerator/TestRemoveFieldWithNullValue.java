package com.psddev.styleguide.viewgenerator;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TestRemoveFieldWithNullValue {

    @Test
    public void testRemoveFieldWithNullValue() throws Exception {

        ViewClassGeneratorContext context = TestUtils.getViewClassGeneratorContextForClass(getClass());
        context.setExcludedPaths(Collections.singleton("base"));

        List<ViewClassDefinition> classDefs = ViewClassDefinition.createDefinitions(context, new JsonDirectory(context).resolveViewMaps());

        Assert.assertEquals(1, classDefs.size());

        List<ViewClassFieldDefinition> fieldDefs = classDefs.iterator().next().getNonNullFieldDefinitions();

        Assert.assertEquals(1, fieldDefs.size());
    }
}
