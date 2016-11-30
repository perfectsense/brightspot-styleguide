package com.psddev.styleguide.viewgenerator;

import java.nio.file.Paths;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TestNoViewConfiguration {

    @Test
    public void testNoViewConfiguration() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        List<ViewConfiguration> configs = jsonDir.getViewConfigurations(Paths.get("foo/bar/baz/qux"));

        Assert.assertEquals(0, configs.size());
    }
}
