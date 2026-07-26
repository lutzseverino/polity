package com.odonta.polity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odonta.polity.api.model.MemberResponse;
import com.odonta.polity.mapper.MembershipAccessTransportMapper;
import com.odonta.polity.mapper.MembershipTransportMapper;
import com.odonta.polity.result.MembershipAccessConvergenceResult;
import com.odonta.polity.result.MembershipAccessConvergenceStatus;
import com.odonta.polity.service.MembershipAccessService;
import com.odonta.polity.service.MembershipService;
import com.odonta.polity.workflow.ResignMembershipWorkflow;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUserReader;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;

class MemberControllerTest {
  private final MembershipAccessService access = mock(MembershipAccessService.class);
  private final AuthenticatedUserReader users = mock(AuthenticatedUserReader.class);
  private final MemberController controller =
      new MemberController(
          mock(MembershipService.class),
          access,
          mock(ResignMembershipWorkflow.class),
          mock(MembershipTransportMapper.class),
          Mappers.getMapper(MembershipAccessTransportMapper.class),
          users);

  @ParameterizedTest
  @EnumSource(MembershipAccessConvergenceStatus.class)
  void selfAccessReadMapsEveryConvergenceStateWithoutExposingItOnGenericMembers(
      MembershipAccessConvergenceStatus status) {
    UUID polityId = UUID.randomUUID();
    AuthenticatedUser user = user();
    MembershipAccessConvergenceResult result = result(status);
    when(users.currentUser()).thenReturn(user);
    when(access.get(polityId, user.id())).thenReturn(result);

    var entity = controller.getCurrentUserMembershipAccess(polityId);

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(entity.getBody()).isNotNull();
    assertThat(entity.getBody().getMembershipId()).isEqualTo(result.membershipId());
    assertThat(entity.getBody().getReceiptId()).isEqualTo(result.receiptId());
    assertThat(entity.getBody().getStatus().getValue())
        .isEqualTo(status.name().toLowerCase(Locale.ROOT));
    assertThat(entity.getBody().getFailureCode()).isEqualTo(result.failureCode());
    assertThat(MemberResponse.class.getDeclaredFields())
        .extracting(Field::getName)
        .doesNotContain("receiptId", "failureCode", "grants", "access");
  }

  @ParameterizedTest
  @EnumSource(MembershipAccessConvergenceStatus.class)
  void selfAccessReconciliationReturnsAcceptedOnlyWhileGrantApplicationIsPending(
      MembershipAccessConvergenceStatus status) {
    UUID polityId = UUID.randomUUID();
    AuthenticatedUser user = user();
    MembershipAccessConvergenceResult result = result(status);
    when(users.currentUser()).thenReturn(user);
    when(access.reconcile(polityId, user.id())).thenReturn(result);

    var entity = controller.reconcileCurrentUserMembershipAccess(polityId);

    assertThat(entity.getStatusCode())
        .isEqualTo(
            status == MembershipAccessConvergenceStatus.PENDING
                ? HttpStatus.ACCEPTED
                : HttpStatus.OK);
    assertThat(entity.getBody()).isNotNull();
    assertThat(entity.getBody().getStatus().getValue())
        .isEqualTo(status.name().toLowerCase(Locale.ROOT));
  }

  private MembershipAccessConvergenceResult result(MembershipAccessConvergenceStatus status) {
    return new MembershipAccessConvergenceResult(
        UUID.randomUUID(),
        status == MembershipAccessConvergenceStatus.LEGACY ? null : UUID.randomUUID(),
        status,
        status == MembershipAccessConvergenceStatus.FAILED ? "provider_application_failed" : null);
  }

  private AuthenticatedUser user() {
    return new AuthenticatedUser(UUID.randomUUID(), "subject:member", "Member");
  }
}
