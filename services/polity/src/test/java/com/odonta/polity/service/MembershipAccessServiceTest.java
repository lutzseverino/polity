package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.model.Membership;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.result.MembershipAccessConvergenceStatus;
import io.github.lutzseverino.cardo.authorization.grant.GrantReceipt;
import io.github.lutzseverino.cardo.authorization.grant.GrantReceiptStatus;
import io.github.lutzseverino.cardo.authorization.grant.Grants;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MembershipAccessServiceTest {
  private final Grants grants = mock(Grants.class);
  private final MembershipRepository memberships = mock(MembershipRepository.class);
  private final MembershipAccessService service =
      new MembershipAccessService(grants, memberships, new PolityGrantPlanner());

  @Test
  void readsLegacyMembershipWithoutTreatingItAsUsableAccess() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Membership membership = membership(polityId, userId);
    when(memberships.findEntityByPolityIdAndUserIdAndStatus(
            polityId, userId, membership.getStatus()))
        .thenReturn(Optional.of(membership));

    var result = service.get(polityId, userId);

    assertThat(result.status()).isEqualTo(MembershipAccessConvergenceStatus.LEGACY);
    assertThat(result.receiptId()).isNull();
    verify(grants, never()).stage(any());
  }

  @Test
  void reconciliationStagesOnceAndRepeatsReturnCurrentReceipt() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID receiptId = UUID.randomUUID();
    Membership membership = membership(polityId, userId);
    GrantReceipt receipt = new GrantReceipt(receiptId, GrantReceiptStatus.PENDING, null);
    when(memberships.findEntityByPolityIdAndUserIdForUpdate(polityId, userId))
        .thenReturn(Optional.of(membership));
    when(grants.stage(any())).thenReturn(receipt);
    when(grants.find(receiptId)).thenReturn(Optional.of(receipt));

    var first = service.reconcile(polityId, userId);
    var repeated = service.reconcile(polityId, userId);

    assertThat(first.status()).isEqualTo(MembershipAccessConvergenceStatus.PENDING);
    assertThat(repeated).isEqualTo(first);
    assertThat(membership.getGrantReceiptId()).isEqualTo(receiptId);
    verify(grants).stage(any());
    verify(memberships).saveAndFlush(membership);
  }

  private Membership membership(UUID polityId, UUID userId) {
    Membership membership =
        new Membership(
            polityId,
            userId,
            "subject:member",
            "member@example.com",
            "Member",
            OffsetDateTime.parse("2026-07-20T12:00:00Z"),
            null);
    ReflectionTestUtils.setField(membership, "id", UUID.randomUUID());
    return membership;
  }
}
