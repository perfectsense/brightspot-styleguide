package com.psddev.styleguide.viewgenerator;

import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

public class TestNoViewConfiguration {

    @Test
    public void testNullPath() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        ViewConfiguration config = jsonDir.getViewConfiguration(Paths.get("foo/bar/baz/qux"));

        Assert.assertNull(config);
    }
}
