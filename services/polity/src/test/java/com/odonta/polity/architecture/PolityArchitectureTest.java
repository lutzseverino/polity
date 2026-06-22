package com.odonta.polity.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "com.odonta.polity",
    importOptions = ImportOption.DoNotIncludeTests.class)
class PolityArchitectureTest {

  @ArchTest
  static final ArchRule controllers_are_not_used_as_collaborators =
      noClasses()
          .that()
          .resideOutsideOfPackage("..controller..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..controller..");

  @ArchTest
  static final ArchRule generated_api_stays_at_transport_boundary =
      noClasses()
          .that()
          .resideOutsideOfPackages("..controller..", "..mapper..", "..exception..", "..api..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.odonta.polity.api..");

  @ArchTest
  static final ArchRule domain_does_not_depend_on_infrastructure =
      noClasses()
          .that()
          .resideInAnyPackage(
              "com.odonta.polity.model..",
              "com.odonta.polity.evaluator..",
              "com.odonta.polity.validation..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..controller..",
              "..mapper..",
              "..repository..",
              "..config..",
              "..service..",
              "com.odonta.polity.authorization..",
              "com.odonta.polity.api..");

  @ArchTest
  static final ArchRule services_do_not_depend_on_transport =
      noClasses()
          .that()
          .resideInAPackage("..service..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..controller..", "com.odonta.polity.api..");

  @ArchTest
  static final ArchRule mappers_do_not_depend_on_orchestration =
      noClasses()
          .that()
          .resideInAPackage("..mapper..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..controller..", "..service..", "..config..");

  @ArchTest
  static final ArchRule repositories_do_not_depend_on_application_layers =
      noClasses()
          .that()
          .resideInAPackage("..repository..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..controller..",
              "..service..",
              "..mapper..",
              "com.odonta.polity.authorization..",
              "com.odonta.polity.api..");
}
