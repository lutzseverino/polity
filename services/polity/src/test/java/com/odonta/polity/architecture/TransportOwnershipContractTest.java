package com.odonta.polity.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class TransportOwnershipContractTest {
  private static final Set<String> HTTP_METHODS =
      Set.of("get", "put", "post", "delete", "options", "head", "patch", "trace");

  private static final Map<String, String> CANONICAL_OPERATION_OWNERS =
      Map.ofEntries(
          Map.entry("getPolityConstitution", "Constitutions"),
          Map.entry("getPolityGovernment", "Government Structures"),
          Map.entry("getPolityActions", "Polity Action Availability"),
          Map.entry("listPolityMembershipInvitations", "Membership Invitations"),
          Map.entry("createPolityMembershipInvitation", "Membership Invitations"),
          Map.entry("listCurrentUserMembershipInvitations", "Membership Invitations"),
          Map.entry("getMembershipInvitationByToken", "Membership Invitations"),
          Map.entry("requestMembershipInvitationCompletion", "Membership Invitations"),
          Map.entry("getMembershipInvitationCompletion", "Membership Invitations"),
          Map.entry("acceptMembershipInvitation", "Membership Invitations"),
          Map.entry("createPolityOfficeElectionMotion", "Office Elections"),
          Map.entry("castPolityOfficeElectionBallot", "Office Elections"),
          Map.entry("respondPolityOfficeElectionCandidacy", "Office Elections"),
          Map.entry("createPolitySanctionMotion", "Sanctions"),
          Map.entry("listPolitySanctions", "Sanctions"),
          Map.entry("createPolityAppealMotion", "Appeals"),
          Map.entry("listPolityAppeals", "Appeals"),
          Map.entry("createPolityOfficeTermReviewMotion", "Office Term Reviews"),
          Map.entry("listPolityOfficeTermReviews", "Office Term Reviews"),
          Map.entry("createPolityConstitutionalReviewMotion", "Constitutional Reviews"),
          Map.entry("listPolityConstitutionalReviews", "Constitutional Reviews"),
          Map.entry("createPolityConstitutionAmendmentMotion", "Constitution Amendments"),
          Map.entry("createPolityDisbandmentMotion", "Disbandments"),
          Map.entry("listPolityOfficeTerms", "Office Terms"));

  @Test
  void operationsUseOneCanonicalTransportOwner() throws IOException {
    Map<String, String> operationOwners = operationOwners(openApiSpecification());

    assertThat(operationOwners).containsAllEntriesOf(CANONICAL_OPERATION_OWNERS);
    assertThat(operationOwners).doesNotContainValue("Justice");
  }

  @Test
  void membershipInvitationSchemasUseCanonicalNames() throws IOException {
    Set<String> schemas =
        map(map(openApiSpecification().get("components")).get("schemas")).keySet();

    assertThat(schemas)
        .contains(
            "CreateMembershipInvitationRequest",
            "MembershipInvitationStatus",
            "MembershipInvitationResponse",
            "MembershipInvitationPageResponse",
            "MembershipInvitationCompletionStatus",
            "MembershipInvitationCompletionResponse")
        .doesNotContain(
            "CreateMemberInvitationRequest",
            "CompleteMembershipInvitationRequest",
            "InvitationStatus",
            "MemberInvitationResponse",
            "MemberInvitationPageResponse");
  }

  private Map<String, String> operationOwners(Map<String, Object> specification) {
    Map<String, String> owners = new LinkedHashMap<>();
    map(specification.get("paths"))
        .values()
        .forEach(
            pathValue ->
                map(pathValue).entrySet().stream()
                    .filter(entry -> HTTP_METHODS.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .map(this::map)
                    .forEach(
                        operation -> {
                          List<String> tags = list(operation.get("tags"));
                          assertThat(tags)
                              .as("transport owners for %s", operation.get("operationId"))
                              .hasSize(1);
                          owners.put((String) operation.get("operationId"), tags.getFirst());
                        }));
    return owners;
  }

  private Map<String, Object> openApiSpecification() throws IOException {
    try (InputStream input = Files.newInputStream(Path.of("openapi/polity.yaml"))) {
      return map(new Yaml().load(input));
    }
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
