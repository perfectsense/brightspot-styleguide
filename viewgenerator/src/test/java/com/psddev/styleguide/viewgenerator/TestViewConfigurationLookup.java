package com.psddev.styleguide.viewgenerator;

import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

public class TestViewConfigurationLookup {

    @Test
    public void testNullPath() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        ViewConfiguration config = jsonDir.getViewConfiguration(null);

        Assert.assertEquals("base", config.getJavaPackage());
    }

    @Test
    public void testConfigsAtEachLevel() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        ViewConfiguration configBase = jsonDir.getViewConfiguration(Paths.get(""));
        ViewConfiguration configFoo = jsonDir.getViewConfiguration(Paths.get("foo"));
        ViewConfiguration configBar = jsonDir.getViewConfiguration(Paths.get("foo/bar"));
        ViewConfiguration configBaz = jsonDir.getViewConfiguration(Paths.get("foo/bar/baz"));

        Assert.assertEquals("base", configBase.getJavaPackage());
        Assert.assertEquals("foo", configFoo.getJavaPackage());
        Assert.assertEquals("foo.bar", configBar.getJavaPackage());
        Assert.assertEquals("foo.bar.baz", configBaz.getJavaPackage());
    }

    @Test
    public void testConfigInMiddle() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        ViewConfiguration configBase = jsonDir.getViewConfiguration(Paths.get(""));
        ViewConfiguration configAlpha = jsonDir.getViewConfiguration(Paths.get("alpha"));
        ViewConfiguration configBeta = jsonDir.getViewConfiguration(Paths.get("alpha/beta"));
        ViewConfiguration configGamma = jsonDir.getViewConfiguration(Paths.get("alpha/beta/gamma"));

        Assert.assertEquals("base", configBase.getJavaPackage());
        Assert.assertEquals("alpha", configAlpha.getJavaPackage());
        Assert.assertEquals("alpha", configBeta.getJavaPackage());
        Assert.assertEquals("alpha", configGamma.getJavaPackage());
    }

    @Test
    public void testConfigInLowest() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        ViewConfiguration configBase = jsonDir.getViewConfiguration(Paths.get(""));
        ViewConfiguration configOne = jsonDir.getViewConfiguration(Paths.get("one"));
        ViewConfiguration configTwo = jsonDir.getViewConfiguration(Paths.get("one/two"));
        ViewConfiguration configThree = jsonDir.getViewConfiguration(Paths.get("one/two/three"));

        Assert.assertEquals("base", configBase.getJavaPackage());
        Assert.assertEquals("base", configOne.getJavaPackage());
        Assert.assertEquals("base", configTwo.getJavaPackage());
        Assert.assertEquals("one.two.three", configThree.getJavaPackage());
    }

    @Test
    public void testNoConfigForPath() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        ViewConfiguration badConfig;

        badConfig = jsonDir.getViewConfiguration(Paths.get("../../"));
        Assert.assertNull(badConfig);

        badConfig = jsonDir.getViewConfiguration(Paths.get("../"));
        Assert.assertNull(badConfig);

        badConfig = jsonDir.getViewConfiguration(Paths.get(".."));
        Assert.assertNull(badConfig);

        badConfig = jsonDir.getViewConfiguration(Paths.get("/alpha/../../"));
        Assert.assertNull(badConfig);
    }

    @Test
    public void testRootConfigForPath() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        ViewConfiguration rootConfig;

        rootConfig = jsonDir.getViewConfiguration(null);
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getViewConfiguration(Paths.get(""));
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getViewConfiguration(Paths.get("."));
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getViewConfiguration(Paths.get("./"));
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getViewConfiguration(Paths.get("/"));
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getViewConfiguration(Paths.get("../TestViewConfigurationLookup"));
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getViewConfiguration(Paths.get("../TestViewConfigurationLookup/"));
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getViewConfiguration(Paths.get("../../resources/TestViewConfigurationLookup/one/two"));
        Assert.assertEquals("base", rootConfig.getJavaPackage());
    }
}
