package com.psddev.styleguide.codegen;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestDataUrlIncludeKey {

    @Test
    public void testDataUrlIncludeKey() throws Exception {

        JsonDirectory directory = TestUtils.getJsonDirectoryForClass(getClass());

        Set<JsonViewMap> viewMaps = directory.resolveViewMaps();

        Assert.assertEquals(3, viewMaps.size());

        List<ViewClassDefinition> definitions = ViewClassDefinition.createDefinitions(directory.getContext(), viewMaps);

        assertEquals(2, definitions.size());
    }
}
