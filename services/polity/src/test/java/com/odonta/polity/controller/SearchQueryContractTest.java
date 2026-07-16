package com.odonta.polity.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class SearchQueryContractTest {
  private static final String SEARCH_QUERY_PARAMETER = "#/components/parameters/SearchQuery";

  @Test
  void politySearchUsesTheSharedCollectionQueryConvention() throws IOException {
    Map<String, Object> specification = openApiSpecification();
    Map<String, Object> components = map(specification.get("components"));
    Map<String, Object> searchQuery = map(map(components.get("parameters")).get("SearchQuery"));

    assertThat(searchQuery)
        .containsEntry("name", "query")
        .containsEntry("in", "query")
        .containsEntry("required", false);
    assertThat(map(searchQuery.get("schema")))
        .containsEntry("type", "string")
        .containsEntry("maxLength", 120);

    Map<String, Object> listPolities =
        map(map(map(specification.get("paths")).get("/polities")).get("get"));
    assertThat(parameterReferences(listPolities)).contains(SEARCH_QUERY_PARAMETER);
  }

  private Map<String, Object> openApiSpecification() throws IOException {
    try (InputStream input = Files.newInputStream(Path.of("openapi/polity.yaml"))) {
      return map(new Yaml().load(input));
    }
  }

  private List<String> parameterReferences(Map<String, Object> operation) {
    return list(operation.get("parameters")).stream()
        .map(this::map)
        .map(parameter -> (String) parameter.get("$ref"))
        .toList();
  }

  @SuppressWarnings("unchecked")
  private <T> List<T> list(Object value) {
    return (List<T>) value;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> map(Object value) {
    return (Map<String, Object>) value;
  }
}
