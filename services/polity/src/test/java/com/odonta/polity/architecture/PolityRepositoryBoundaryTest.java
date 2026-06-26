package com.odonta.polity.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import org.junit.jupiter.api.Test;

class PolityRepositoryBoundaryTest {

  @Test
  void entityMaterializingRepositoryMethodsAreExplicit() {
    repositoryClasses().stream()
        .flatMap(repository -> java.util.Arrays.stream(repository.getDeclaredMethods()))
        .filter(method -> containsEntity(method.getGenericReturnType()))
        .forEach(
            method ->
                assertThat(method.getName())
                    .as(method.toString())
                    .matches("find(Top)?Entit(y|ies).*"));
  }

  private Collection<Class<?>> repositoryClasses() {
    return new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.odonta.polity.repository")
            .stream()
            .filter(JavaClass::isInterface)
            .filter(type -> type.getSimpleName().endsWith("Repository"))
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

  private boolean containsEntity(Type type) {
    if (type instanceof Class<?> value) {
      return value.isAnnotationPresent(Entity.class);
    }
    if (type instanceof ParameterizedType value) {
      return containsEntity(value.getRawType())
          || java.util.Arrays.stream(value.getActualTypeArguments()).anyMatch(this::containsEntity);
    }
    return false;
  }
}
