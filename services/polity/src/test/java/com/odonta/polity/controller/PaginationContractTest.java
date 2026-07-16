package com.odonta.polity.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odonta.polity.result.PageResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class PaginationContractTest {
  private static final String PAGE_PARAMETER = "#/components/parameters/Page";
  private static final String PAGE_SIZE_PARAMETER = "#/components/parameters/PageSize";
  private static final String SCHEMA_PREFIX = "#/components/schemas/";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void serializesPageContentAndMetadataInTheResponseBody() throws Exception {
    var response =
        PageResponses.ok(new PageResult<>(List.of("item"), 1, 25, 26), items -> List.copyOf(items));

    var json = objectMapper.readTree(objectMapper.writeValueAsBytes(response.getBody()));

    assertThat(json.path("content").get(0).asText()).isEqualTo("item");
    assertThat(json.path("page").path("number").asInt()).isEqualTo(1);
    assertThat(json.path("page").path("size").asInt()).isEqualTo(25);
    assertThat(json.path("page").path("totalElements").asLong()).isEqualTo(26);
    assertThat(json.path("page").path("totalPages").asInt()).isEqualTo(2);
  }

  @Test
  void everyPaginatedOperationUsesThePageResponseConvention() throws IOException {
    Map<String, Object> specification = openApiSpecification();
    Map<String, Object> schemas = map(map(specification.get("components")).get("schemas"));
    List<Map<String, Object>> operations = paginatedOperations(specification);

    assertThat(operations).isNotEmpty();
    for (Map<String, Object> operation : operations) {
      assertThat(parameterReferences(operation)).contains(PAGE_SIZE_PARAMETER);

      Map<String, Object> okResponse = map(map(operation.get("responses")).get("200"));
      assertThat(okResponse).doesNotContainKey("headers");

      String responseSchema = responseSchemaReference(okResponse);
      assertThat(responseSchema).startsWith(SCHEMA_PREFIX).endsWith("PageResponse");

      Map<String, Object> pageResponse =
          map(schemas.get(responseSchema.substring(SCHEMA_PREFIX.length())));
      assertThat(list(pageResponse.get("required"))).containsExactly("content", "page");

      Map<String, Object> properties = map(pageResponse.get("properties"));
      assertThat(map(properties.get("content"))).containsEntry("type", "array");
      assertThat(map(properties.get("page")))
          .containsEntry("$ref", "#/components/schemas/PageMetadata");
    }

    Map<String, Object> metadata = map(schemas.get("PageMetadata"));
    assertThat(list(metadata.get("required")))
        .containsExactly("size", "number", "totalElements", "totalPages");
  }

  private Map<String, Object> openApiSpecification() throws IOException {
    try (InputStream input = Files.newInputStream(Path.of("openapi/polity.yaml"))) {
      return map(new Yaml().load(input));
    }
  }

  private List<Map<String, Object>> paginatedOperations(Map<String, Object> specification) {
    return map(specification.get("paths")).values().stream()
        .map(this::map)
        .flatMap(path -> path.values().stream())
        .filter(Map.class::isInstance)
        .map(this::map)
        .filter(operation -> parameterReferences(operation).contains(PAGE_PARAMETER))
        .toList();
  }

  private List<String> parameterReferences(Map<String, Object> operation) {
    Object parameters = operation.get("parameters");
    if (!(parameters instanceof List<?> values)) {
      return List.of();
    }
    return values.stream().map(this::map).map(parameter -> (String) parameter.get("$ref")).toList();
  }

  private String responseSchemaReference(Map<String, Object> response) {
    Map<String, Object> content = map(response.get("content"));
    Map<String, Object> mediaType = map(content.get("application/json"));
    return (String) map(mediaType.get("schema")).get("$ref");
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
