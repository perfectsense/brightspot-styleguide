package com.psddev.styleguide.viewgenerator;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class TestDataUrlSimple {

    @Test
    public void testJsonDirectory() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        Set<JsonViewMap> viewMaps = jsonDir.resolveViewMaps();

        Assert.assertEquals(8, viewMaps.size());
    }
}
