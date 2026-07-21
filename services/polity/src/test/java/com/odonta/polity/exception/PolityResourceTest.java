package com.odonta.polity.exception;

import static com.odonta.polity.exception.RequiredResource.required;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import io.github.lutzseverino.cardo.common.api.ApiException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PolityResourceTest {
  @Test
  void definesCanonicalNotFoundProblemsForEverySharedResource() {
    assertThat(PolityResource.values())
        .extracting(PolityResource::notFoundCode, PolityResource::notFoundMessage)
        .containsExactly(
            tuple("member_not_found", "Member not found."),
            tuple("polity_account_not_found", "Polity account not found."),
            tuple("office_not_found", "Office not found."),
            tuple("polity_not_found", "Polity not found."),
            tuple("constitution_not_found", "Constitution not found."),
            tuple("jurisdiction_not_found", "Jurisdiction not found."),
            tuple("institution_not_found", "Institution not found."),
            tuple("motion_not_found", "Motion not found."),
            tuple("procedure_not_found", "Procedure not found."),
            tuple("office_term_not_found", "Office term not found."),
            tuple("sanction_not_found", "Sanction not found."),
            tuple("invitation_not_found", "Invitation not found."),
            tuple("power_not_found", "Constitutional power not found."),
            tuple("official_record_entry_not_found", "Official record entry not found."));
  }

  @Test
  void requiredReturnsTheMappedResource() {
    assertThat(required(Map.of("member", 42), "member", PolityResource.MEMBER)).isEqualTo(42);
  }

  @Test
  void requiredThrowsTheSelectedCanonicalProblemWhenTheResourceIsMissing() {
    assertThatThrownBy(() -> required(Map.of(), "member", PolityResource.MEMBER))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> {
              assertThat(exception.code()).isEqualTo("member_not_found");
              assertThat(exception.getMessage()).isEqualTo("Member not found.");
            });
  }

  @Test
  void canonicalResourceProblemsCannotBeAuthoredAsDirectApiExceptionLiterals() throws IOException {
    String canonicalCodes =
        Arrays.stream(PolityResource.values())
            .map(PolityResource::notFoundCode)
            .collect(Collectors.joining("|"));
    Pattern directCanonicalProblem =
        Pattern.compile("ApiException\\.notFound\\(\\s*\"(?:" + canonicalCodes + ")\"");

    List<Path> violations;
    try (var files =
        Files.find(
            Path.of("src/main/java"),
            Integer.MAX_VALUE,
            (path, attrs) -> attrs.isRegularFile() && path.toString().endsWith(".java"))) {
      violations =
          files
              .filter(
                  path -> {
                    try {
                      return directCanonicalProblem.matcher(Files.readString(path)).find();
                    } catch (IOException exception) {
                      throw new UncheckedIOException(exception);
                    }
                  })
              .toList();
    }

    assertThat(violations).isEmpty();
  }
}
