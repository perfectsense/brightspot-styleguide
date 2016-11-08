package com.psddev.styleguide.viewgenerator;

import org.junit.Test;

public class TestConflictingPackageNames {

    @Test(expected = RuntimeException.class)
    public void testDataUrlComplex() throws Exception {

        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());

        generator.getGeneratedClasses();
    }
}
