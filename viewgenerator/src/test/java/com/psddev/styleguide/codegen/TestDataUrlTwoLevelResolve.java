package com.psddev.styleguide.codegen;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class TestDataUrlTwoLevelResolve {

    @Test
    public void testDataUrlTwoLevelResolve() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        Set<JsonViewMap> viewMaps = jsonDir.resolveViewMaps();

        Assert.assertEquals(3, viewMaps.size());
    }
}
