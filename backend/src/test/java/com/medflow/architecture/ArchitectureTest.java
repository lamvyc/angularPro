package com.medflow.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 分层约束：领域层不依赖框架，API 层不接触基础设施。
 */
@AnalyzeClasses(packages = "com.medflow")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_framework_or_outer_layers = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..api..", "..infrastructure..",
                    "org.springframework.web..", "com.baomidou..", "org.apache.ibatis..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule api_should_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllers_should_not_inject_mappers = noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Mapper")
            .allowEmptyShould(true);
}
