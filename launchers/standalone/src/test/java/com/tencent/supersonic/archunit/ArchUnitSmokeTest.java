package com.tencent.supersonic.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchUnitSmokeTest {

    @Test
    void canImportSupersonicClasses() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.tencent.supersonic");

        assertTrue(classes.size() > 500, "Expected >500 imported classes but got " + classes.size()
                + ". Is launchers-standalone missing a module dep?");
    }
}
