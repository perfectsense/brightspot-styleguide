package com.psddev.styleguide.codegen;

import org.junit.Test;

public class TestConflictingPackageNames {

    @Test(expected = ViewClassGeneratorException.class)
    public void testConflictingPackageNames() throws Exception {

        ViewClassGenerator generator = TestUtils.getDefaultGeneratorForClass(getClass());

        generator.getGeneratedClasses();
    }
}
