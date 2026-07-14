package com.odonta.polity.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Service;

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
              "..effect..",
              "..mapper..",
              "..repository..",
              "..resolver..",
              "..config..",
              "..service..",
              "com.odonta.polity.authorization..",
              "com.odonta.polity.api..");

  @ArchTest
  static final ArchRule persistent_domain_does_not_depend_on_application_inputs_or_results =
      noClasses()
          .that()
          .resideInAPackage("com.odonta.polity.model..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("com.odonta.polity.input..", "com.odonta.polity.result..");

  @ArchTest
  static final ArchRule application_inputs_and_results_do_not_depend_on_application_owners =
      noClasses()
          .that()
          .resideInAnyPackage("com.odonta.polity.input..", "com.odonta.polity.result..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..controller..",
              "..effect..",
              "..mapper..",
              "..repository..",
              "..resolver..",
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
  static final ArchRule controllers_do_not_access_repositories =
      noClasses()
          .that()
          .resideInAPackage("..controller..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..repository..");

  @ArchTest
  static final ArchRule resolvers_do_not_apply_effects =
      noClasses()
          .that()
          .resideInAPackage("..resolver..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..effect..");

  @ArchTest
  static final ArchRule effects_do_not_depend_on_transport =
      noClasses()
          .that()
          .resideInAPackage("..effect..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..controller..", "..mapper..", "com.odonta.polity.api..");

  @ArchTest
  static final ArchRule service_package_contains_application_services =
      classes()
          .that()
          .resideInAPackage("..service..")
          .and()
          .areTopLevelClasses()
          .should()
          .beAnnotatedWith(Service.class);

  @ArchTest
  static final ArchRule application_mappers_stay_inside_the_application_boundary =
      noClasses()
          .that()
          .haveSimpleNameEndingWith("ApplicationMapper")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("com.odonta.polity.api..", "..controller..", "..service..");

  @ArchTest
  static final ArchRule transport_mappers_do_not_depend_on_application_or_persistence_owners =
      noClasses()
          .that()
          .haveSimpleNameEndingWith("TransportMapper")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..repository..", "..resolver..", "..service..");

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
