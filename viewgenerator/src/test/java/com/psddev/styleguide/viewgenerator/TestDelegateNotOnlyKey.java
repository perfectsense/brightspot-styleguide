package com.psddev.styleguide.viewgenerator;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class TestDelegateNotOnlyKey {

    @Test(expected = ViewClassGeneratorException.class)
    public void testDelegateNotOnlyKey() throws Exception {

        // get a default test view class generator
        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        jsonDir.resolveViewMaps();

        Set<JsonFile> files = jsonDir.getFiles();

        Assert.assertEquals(1, files.size());

        JsonFile file = files.iterator().next();

        Assert.assertEquals(2, file.getErrors().size());
    }
}
