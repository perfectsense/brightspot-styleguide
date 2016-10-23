package com.psddev.styleguide.viewgenerator;

import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

public class TestViewConfigurationLookup {

    @Test
    public void testNullPath() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        TemplateViewConfiguration config = jsonDir.getTemplateViewConfiguration(null);

        Assert.assertEquals("base", config.getJavaPackage());
    }

    @Test
    public void testConfigsAtEachLevel() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        TemplateViewConfiguration configBase = jsonDir.getTemplateViewConfiguration(Paths.get(""));
        TemplateViewConfiguration configFoo = jsonDir.getTemplateViewConfiguration(Paths.get("foo"));
        TemplateViewConfiguration configBar = jsonDir.getTemplateViewConfiguration(Paths.get("foo/bar"));
        TemplateViewConfiguration configBaz = jsonDir.getTemplateViewConfiguration(Paths.get("foo/bar/baz"));

        Assert.assertEquals("base", configBase.getJavaPackage());
        Assert.assertEquals("foo", configFoo.getJavaPackage());
        Assert.assertEquals("foo.bar", configBar.getJavaPackage());
        Assert.assertEquals("foo.bar.baz", configBaz.getJavaPackage());
    }

    @Test
    public void testConfigInMiddle() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        TemplateViewConfiguration configBase = jsonDir.getTemplateViewConfiguration(Paths.get(""));
        TemplateViewConfiguration configAlpha = jsonDir.getTemplateViewConfiguration(Paths.get("alpha"));
        TemplateViewConfiguration configBeta = jsonDir.getTemplateViewConfiguration(Paths.get("alpha/beta"));
        TemplateViewConfiguration configGamma = jsonDir.getTemplateViewConfiguration(Paths.get("alpha/beta/gamma"));

        Assert.assertEquals("base", configBase.getJavaPackage());
        Assert.assertEquals("alpha", configAlpha.getJavaPackage());
        Assert.assertEquals("alpha", configBeta.getJavaPackage());
        Assert.assertEquals("alpha", configGamma.getJavaPackage());
    }

    @Test
    public void testConfigInLowest() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        TemplateViewConfiguration configBase = jsonDir.getTemplateViewConfiguration(Paths.get(""));
        TemplateViewConfiguration configOne = jsonDir.getTemplateViewConfiguration(Paths.get("one"));
        TemplateViewConfiguration configTwo = jsonDir.getTemplateViewConfiguration(Paths.get("one/two"));
        TemplateViewConfiguration configThree = jsonDir.getTemplateViewConfiguration(Paths.get("one/two/three"));

        Assert.assertEquals("base", configBase.getJavaPackage());
        Assert.assertEquals("base", configOne.getJavaPackage());
        Assert.assertEquals("base", configTwo.getJavaPackage());
        Assert.assertEquals("one.two.three", configThree.getJavaPackage());
    }

    @Test
    public void testNoConfigForPath() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        TemplateViewConfiguration badConfig;

        badConfig = jsonDir.getTemplateViewConfiguration(Paths.get("../../"));
        Assert.assertNull(badConfig);

        badConfig = jsonDir.getTemplateViewConfiguration(Paths.get("../"));
        Assert.assertNull(badConfig);

        badConfig = jsonDir.getTemplateViewConfiguration(Paths.get(".."));
        Assert.assertNull(badConfig);

        badConfig = jsonDir.getTemplateViewConfiguration(Paths.get("/alpha/../../"));
        Assert.assertNull(badConfig);
    }

    @Test
    public void testRootConfigForPath() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        TemplateViewConfiguration rootConfig;

        rootConfig = jsonDir.getTemplateViewConfiguration(null);
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getTemplateViewConfiguration(Paths.get(""));
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getTemplateViewConfiguration(Paths.get("."));
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getTemplateViewConfiguration(Paths.get("./"));
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getTemplateViewConfiguration(Paths.get("/"));
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getTemplateViewConfiguration(Paths.get("../TestViewConfigurationLookup"));
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getTemplateViewConfiguration(Paths.get("../TestViewConfigurationLookup/"));
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getTemplateViewConfiguration(Paths.get("../../resources/TestViewConfigurationLookup/one/two"));
        Assert.assertEquals("base", rootConfig.getJavaPackage());
    }
}
