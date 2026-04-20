package com.tencent.supersonic.archunit;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Enforces SuperSonic's inter-module dependency rules. See
 * docs/details/platform/04-module-boundaries.md. If a rule fires, fix the offending class — do NOT
 * relax the rule.
 */
@AnalyzeClasses(packages = "com.tencent.supersonic",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryTest {

    // Rules added below

}
