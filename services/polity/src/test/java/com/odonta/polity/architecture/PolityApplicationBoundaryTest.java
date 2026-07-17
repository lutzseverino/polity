package com.odonta.polity.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

class PolityApplicationBoundaryTest {

  @Test
  void applicationOwnerContractsExcludeTransportAndPersistenceTypes() {
    applicationOwners().stream()
        .flatMap(owner -> Arrays.stream(owner.getDeclaredMethods()))
        .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
        .forEach(
            method -> {
              assertThat(forbidden(method.getGenericReturnType())).as(method.toString()).isFalse();
              Arrays.stream(method.getGenericParameterTypes())
                  .forEach(type -> assertThat(forbidden(type)).as(method.toString()).isFalse());
            });
  }

  @Test
  void serviceStereotypeNamesApplicationServiceBoundaries() {
    applicationServices()
        .forEach(
            service -> {
              assertThat(service.getSimpleName()).as(service.getName()).endsWith("Service");
              assertThat(java.lang.reflect.Modifier.isPublic(service.getModifiers()))
                  .as(service.getName())
                  .isTrue();
            });
  }

  private Collection<Class<?>> applicationServices() {
    return new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.odonta.polity.service")
            .stream()
            .filter(type -> type.isAnnotatedWith(Service.class))
            .map(this::reflect)
            .toList();
  }

  private Collection<Class<?>> applicationOwners() {
    return java.util.stream.Stream.concat(
            applicationServices().stream(), publicWorkflowEntrypoints().stream())
        .toList();
  }

  private Collection<Class<?>> publicWorkflowEntrypoints() {
    return new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.odonta.polity.workflow")
            .stream()
            .filter(
                type ->
                    type.getModifiers()
                        .contains(com.tngtech.archunit.core.domain.JavaModifier.PUBLIC))
            .map(this::reflect)
            .toList();
  }

  private Class<?> reflect(JavaClass type) {
    try {
      return Class.forName(type.getName());
    } catch (ClassNotFoundException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private boolean forbidden(Type type) {
    if (type instanceof Class<?> value) {
      return value.getPackageName().contains(".api.model")
          || value.getSimpleName().endsWith("Projection")
          || value.isAnnotationPresent(Entity.class);
    }
    if (type instanceof ParameterizedType value) {
      return forbidden(value.getRawType())
          || Arrays.stream(value.getActualTypeArguments()).anyMatch(this::forbidden);
    }
    return false;
  }
}
