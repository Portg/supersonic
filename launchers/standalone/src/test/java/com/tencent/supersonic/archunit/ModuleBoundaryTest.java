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

    // Rule 1: intentionally kept as a specific callout even though Rule 2 subsumes it,
    // because the because-clause references the migration doc for discoverability.
    @ArchTest
    static final ArchRule headlessApi_shouldNotDependOn_headlessServer = noClasses().that()
            .resideInAPackage("..headless.api..").should().dependOnClassesThat()
            .resideInAPackage("..headless.server..")
            .because("headless-api is a pure contract module; "
                    + "headless-server holds MyBatis DOs and Spring beans. "
                    + "See docs/superpowers/plans/2026-04-14-headless-dto-boundary-migration.md.");

    @ArchTest
    static final ArchRule apiPackages_shouldNotDependOn_serverPackages = noClasses().that()
            .resideInAPackage("..(*).api..").should().dependOnClassesThat()
            .resideInAnyPackage("..chat.server..", "..headless.server..", "..feishu.server..",
                    "..billing.server..", "..auth.authentication..", "..auth.authorization..")
            .because("*.api modules must be pure contracts "
                    + "(POJOs + service interfaces). Server-only "
                    + "implementations must stay in *.server or auth impl packages.");

    @ArchTest
    static final ArchRule authApi_shouldNotDependOn_authImpl =
            noClasses().that().resideInAPackage("..auth.api..").should().dependOnClassesThat()
                    .resideInAnyPackage("..auth.authentication..", "..auth.authorization..")
                    .because("auth-api is the public contract; "
                            + "auth-authentication and auth-authorization are "
                            + "the implementation modules.");

    @ArchTest
    static final ArchRule chatServer_shouldNotDependOn_headlessServer =
            noClasses().that().resideInAPackage("..chat.server..").should().dependOnClassesThat()
                    .resideInAPackage("..headless.server..")
                    .because("chat-server must consume headless through "
                            + "headless-api/headless-chat/headless-core, not "
                            + "headless-server persistence or Spring internals.");

    @ArchTest
    static final ArchRule headlessChat_shouldNotDependOn_headlessServer =
            noClasses().that().resideInAPackage("..headless.chat..").should().dependOnClassesThat()
                    .resideInAPackage("..headless.server..")
                    .because("headless-chat is reusable semantic parsing logic. "
                            + "It must not depend on headless-server persistence "
                            + "or Spring internals.");

    // feishu-server is a delivery channel — it may only consume other modules via their .api
    // contracts, not via server-layer internals. This is a blacklist of known server packages;
    // see docs/details/platform/04-module-boundaries.md for the full rationale.
    @ArchTest
    static final ArchRule feishuServer_shouldNotDependOn_serverInternals =
            noClasses().that().resideInAPackage("..feishu.server..").should().dependOnClassesThat()
                    .resideInAnyPackage("..chat.server..", "..headless.server..",
                            "..headless.chat..", "..headless.core..", "..billing.server..",
                            "..auth.authentication..", "..auth.authorization..")
                    .because("feishu-server is a delivery channel. It must "
                            + "consume chat / headless / auth / billing only via their .api "
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
                    + "to a sibling module creates a circular Maven reactor graph.");

    // Launchers (com.tencent.supersonic.config) are the composition root and are explicitly
    // allowed to wire server-layer beans across modules. They are excluded from boundary rules.
    @ArchTest
    static final ArchRule apiPackages_shouldNotContain_springStereotypes = noClasses().that()
            .resideInAPackage("..(*).api..").should()
            .beAnnotatedWith("org.springframework.stereotype.Component").orShould()
            .beAnnotatedWith("org.springframework.stereotype.Service").orShould()
            .beAnnotatedWith("org.springframework.stereotype.Repository").orShould()
            .beAnnotatedWith("org.springframework.stereotype.Controller").orShould()
            .beAnnotatedWith("org.springframework.web.bind.annotation.RestController").orShould()
            .beAnnotatedWith("org.springframework.context.annotation.Configuration").orShould()
            .beAnnotatedWith("org.springframework.web.bind.annotation.ControllerAdvice").orShould()
            .beAnnotatedWith("org.springframework.web.bind.annotation.RestControllerAdvice")
            .because("*.api modules must be pure contracts. "
                    + "Classes annotated with Spring stereotypes or @Configuration "
                    + "belong in *.server (or authentication/authorization).");

    @ArchTest
    static final ArchRule noCyclesBetweenTopLevelModules =
            slices().matching("..supersonic.(*)..").should().beFreeOfCycles();
}
