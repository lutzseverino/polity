package com.odonta.polity.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(
    packages = "com.odonta.polity",
    importOptions = ImportOption.DoNotIncludeTests.class)
class TransportControllerArchitectureTest {
  private static final String GENERATED_API_PACKAGE = "com.odonta.polity.api";

  @ArchTest
  static final ArchRule controllers_implement_exactly_one_generated_api_owner =
      classes()
          .that()
          .areAnnotatedWith(RestController.class)
          .should(implementExactlyOneGeneratedApi());

  private static ArchCondition<JavaClass> implementExactlyOneGeneratedApi() {
    return new ArchCondition<>("implement exactly one generated API owner") {
      @Override
      public void check(JavaClass controller, ConditionEvents events) {
        long apiCount =
            controller.getRawInterfaces().stream()
                .filter(api -> api.getPackageName().equals(GENERATED_API_PACKAGE))
                .count();
        events.add(
            new SimpleConditionEvent(
                controller,
                apiCount == 1,
                "%s implements %d generated API owners".formatted(controller.getName(), apiCount)));
      }
    };
  }
}
