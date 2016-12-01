package com.psddev.styleguide.codegen;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class TestJavaPackageNameGeneration {

    @Test
    public void testJavaPackageNameGeneration() throws Exception {

        Map<Path, String> pathToExpectedPackage = new LinkedHashMap<>();

        for (String[] data : new String[][] {
                // control
                {"/foo/bar/baz/Main.hbs", "foo.bar.baz"},

                // java keyword
                {"/int/example/Main.hbs", "int_.example"},

                // digit start
                {"/foo/0/bar/Main.hbs", "foo._0.bar"},

                // special character
                {"/foo/bar-baz/qux/Main.hbs", "foo.bar_baz.qux"}}) {

            pathToExpectedPackage.put(Paths.get(data[0]), data[1]);
        }

        for (Map.Entry<Path, String> entry : pathToExpectedPackage.entrySet()) {
            Assert.assertEquals(entry.getValue(), ViewClassStringUtils.toJavaPackageName(entry.getKey()));
        }
    }
}
