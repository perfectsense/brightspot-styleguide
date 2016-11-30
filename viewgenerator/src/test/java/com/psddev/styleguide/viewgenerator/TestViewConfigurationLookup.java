package com.psddev.styleguide.viewgenerator;

import java.nio.file.Paths;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TestViewConfigurationLookup {

    @Test
    public void testNullPath() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        TemplateViewConfiguration config = jsonDir.getViewConfigurations(null).get(0);

        Assert.assertEquals("base", config.getJavaPackage());
    }

    @Test
    public void testConfigsAtEachLevel() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        TemplateViewConfiguration configBase = jsonDir.getViewConfigurations(Paths.get("")).get(0);
        TemplateViewConfiguration configFoo = jsonDir.getViewConfigurations(Paths.get("foo")).get(0);
        TemplateViewConfiguration configBar = jsonDir.getViewConfigurations(Paths.get("foo/bar")).get(0);
        TemplateViewConfiguration configBaz = jsonDir.getViewConfigurations(Paths.get("foo/bar/baz")).get(0);

        Assert.assertEquals("base", configBase.getJavaPackage());
        Assert.assertEquals("foo", configFoo.getJavaPackage());
        Assert.assertEquals("foo.bar", configBar.getJavaPackage());
        Assert.assertEquals("foo.bar.baz", configBaz.getJavaPackage());
    }

    @Test
    public void testConfigInMiddle() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        TemplateViewConfiguration configBase = jsonDir.getViewConfigurations(Paths.get("")).get(0);
        TemplateViewConfiguration configAlpha = jsonDir.getViewConfigurations(Paths.get("alpha")).get(0);
        TemplateViewConfiguration configBeta = jsonDir.getViewConfigurations(Paths.get("alpha/beta")).get(0);
        TemplateViewConfiguration configGamma = jsonDir.getViewConfigurations(Paths.get("alpha/beta/gamma")).get(0);

        Assert.assertEquals("base", configBase.getJavaPackage());
        Assert.assertEquals("alpha", configAlpha.getJavaPackage());
        Assert.assertEquals("alpha", configBeta.getJavaPackage());
        Assert.assertEquals("alpha", configGamma.getJavaPackage());
    }

    @Test
    public void testConfigInLowest() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        TemplateViewConfiguration configBase = jsonDir.getViewConfigurations(Paths.get("")).get(0);
        TemplateViewConfiguration configOne = jsonDir.getViewConfigurations(Paths.get("one")).get(0);
        TemplateViewConfiguration configTwo = jsonDir.getViewConfigurations(Paths.get("one/two")).get(0);
        TemplateViewConfiguration configThree = jsonDir.getViewConfigurations(Paths.get("one/two/three")).get(0);

        Assert.assertEquals("base", configBase.getJavaPackage());
        Assert.assertEquals("base", configOne.getJavaPackage());
        Assert.assertEquals("base", configTwo.getJavaPackage());
        Assert.assertEquals("one.two.three", configThree.getJavaPackage());
    }

    @Test
    public void testNoConfigForPath() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        List<TemplateViewConfiguration> badConfigs;

        badConfigs = jsonDir.getViewConfigurations(Paths.get("../../"));
        Assert.assertEquals(0, badConfigs.size());

        badConfigs = jsonDir.getViewConfigurations(Paths.get("../"));
        Assert.assertEquals(0, badConfigs.size());

        badConfigs = jsonDir.getViewConfigurations(Paths.get(".."));
        Assert.assertEquals(0, badConfigs.size());

        badConfigs = jsonDir.getViewConfigurations(Paths.get("/alpha/../../"));
        Assert.assertEquals(0, badConfigs.size());
    }

    @Test
    public void testRootConfigForPath() throws Exception {

        JsonDirectory jsonDir = TestUtils.getJsonDirectoryForClass(getClass());

        TemplateViewConfiguration rootConfig;

        rootConfig = jsonDir.getViewConfigurations(null).get(0);
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getViewConfigurations(Paths.get("")).get(0);
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getViewConfigurations(Paths.get(".")).get(0);
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getViewConfigurations(Paths.get("./")).get(0);
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getViewConfigurations(Paths.get("/")).get(0);
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getViewConfigurations(Paths.get("../TestViewConfigurationLookup")).get(0);
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getViewConfigurations(Paths.get("../TestViewConfigurationLookup/")).get(0);
        Assert.assertEquals("base", rootConfig.getJavaPackage());

        rootConfig = jsonDir.getViewConfigurations(Paths.get("../../resources/TestViewConfigurationLookup/one/two")).get(0);
        Assert.assertEquals("base", rootConfig.getJavaPackage());
    }
}
