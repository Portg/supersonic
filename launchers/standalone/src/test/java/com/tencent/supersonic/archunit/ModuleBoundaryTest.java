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

    @ArchTest
    static final ArchRule headlessApi_shouldNotDependOn_headlessServer =
            noClasses().that().resideInAPackage("..headless.api..").should().dependOnClassesThat()
                    .resideInAPackage("..headless.server..")
                    .because("headless-api is a pure contract module; "
                            + "headless-server holds MyBatis DOs and Spring beans. "
                            + "See 2026-04-14-headless-dto-boundary-migration.md.");

    @ArchTest
    static final ArchRule apiPackages_shouldNotDependOn_serverPackages =
            noClasses().that().resideInAPackage("..(*).api..").should().dependOnClassesThat()
                    .resideInAnyPackage("..chat.server..", "..headless.server..",
                            "..feishu.server..", "..billing.server..")
                    .because("*.api modules must be pure contracts "
                            + "(POJOs + service interfaces). Server-only "
                            + "implementations must stay in *.server.");

    @ArchTest
    static final ArchRule authApi_shouldNotDependOn_authImpl =
            noClasses().that().resideInAPackage("..auth.api..").should().dependOnClassesThat()
                    .resideInAnyPackage("..auth.authentication..", "..auth.authorization..")
                    .because("auth-api is the public contract; "
                            + "auth-authentication and auth-authorization are "
                            + "the implementation modules.");

    @ArchTest
    static final ArchRule feishuServer_shouldNotDependOn_chatOrHeadlessServer =
            noClasses().that().resideInAPackage("..feishu.server..").should().dependOnClassesThat()
                    .resideInAnyPackage("..chat.server..", "..headless.server..")
                    .because("feishu-server is a delivery channel. It must "
                            + "consume chat / headless only via their .api "
                            + "modules. Direct server deps were removed in "
                            + "the 2026-04-14 boundary migration.");

    @ArchTest
    static final ArchRule chatServer_shouldNotDependOn_feishuServer =
            noClasses().that().resideInAPackage("..chat.server..").should().dependOnClassesThat()
                    .resideInAPackage("..feishu.server..")
                    .because("chat-server must not know about feishu-server. "
                            + "Cross-module notifications go via Spring "
                            + "ApplicationEvent (see CLAUDE.md).");

    @ArchTest
    static final ArchRule common_shouldNotDependOn_anySibling = noClasses().that()
            .resideInAPackage("com.tencent.supersonic.common..").should().dependOnClassesThat()
            .resideInAnyPackage("com.tencent.supersonic.auth..", "com.tencent.supersonic.billing..",
                    "com.tencent.supersonic.chat..", "com.tencent.supersonic.feishu..",
                    "com.tencent.supersonic.headless..")
            .because("common is the shared base. Any outbound dep "
                    + "to a sibling module creates a circular " + "Maven reactor graph.");

    @ArchTest
    static final ArchRule apiPackages_shouldNotContain_springStereotypes =
            noClasses().that().resideInAPackage("..(*).api..").should()
                    .beAnnotatedWith("org.springframework.stereotype.Component").orShould()
                    .beAnnotatedWith("org.springframework.stereotype.Service").orShould()
                    .beAnnotatedWith("org.springframework.stereotype.Repository").orShould()
                    .beAnnotatedWith("org.springframework.stereotype.Controller").orShould()
                    .beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .because("*.api modules must be pure contracts. "
                            + "Classes annotated with Spring stereotypes "
                            + "belong in *.server (or authentication/authorization).");

    @ArchTest
    static final ArchRule noCyclesBetweenTopLevelModules =
            slices().matching("..supersonic.(*)..").should().beFreeOfCycles();
}
