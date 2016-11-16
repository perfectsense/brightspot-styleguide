package com.psddev.styleguide.viewgenerator;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class TestMultipleDataUrls {

    @Test
    public void testJsonDirectory() throws Exception {
        // Should throw and error
        Set<JsonViewMap> viewMaps = TestUtils.getJsonDirectoryForClass(getClass()).resolveViewMaps();
        Assert.assertEquals(17, viewMaps.size());
    }
}
