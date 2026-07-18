package com.odonta.polity.workflow;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.PolityResources;
import com.odonta.polity.authorization.PolityGrantPlanner;
import com.odonta.polity.input.CreatePolityInput;
import com.odonta.polity.model.PolityStatus;
import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.repository.ConstitutionVersionRepository;
import com.odonta.polity.repository.JurisdictionRepository;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.OfficeRepository;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.resolver.PolitySummaryResolver;
import com.odonta.polity.service.OfficialRecordService;
import com.odonta.polity.service.PolitySlugService;
import com.odonta.polity.template.ConstitutionTemplateSeeder;
import io.github.lutzseverino.cardo.authorization.grant.Grants;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.billing.client.BillingEntitlement;
import io.github.lutzseverino.cardo.billing.client.BillingEntitlementStatus;
import io.github.lutzseverino.cardo.billing.client.BillingEntitlementsClient;
import io.github.lutzseverino.cardo.common.api.ApiException;
import io.github.lutzseverino.cardo.identity.client.IdentityUsersClient;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreatePolityWorkflowTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-06-18T12:00:00Z");

  private final BillingEntitlementsClient entitlements = mock(BillingEntitlementsClient.class);
  private final Grants grants = mock(Grants.class);
  private final IdentityUsersClient identityUsers = mock(IdentityUsersClient.class);
  private final PolityRepository polities = mock(PolityRepository.class);
  private final CreatePolityWorkflow workflow =
      new CreatePolityWorkflow(
          Clock.fixed(NOW.toInstant(), ZoneOffset.UTC),
          entitlements,
          mock(ConstitutionTemplateSeeder.class),
          mock(ConstitutionVersionRepository.class),
          grants,
          identityUsers,
          mock(JurisdictionRepository.class),
          mock(MembershipRepository.class),
          mock(OfficeRepository.class),
          mock(OfficeTermRepository.class),
          mock(OfficialRecordService.class),
          new PolityGrantPlanner(),
          polities,
          mock(PolitySlugService.class),
          mock(PolitySummaryResolver.class));

  @Test
  void createRejectsPrivatePolityWhenEntitlementLimitIsReached() {
    UUID founderId = UUID.randomUUID();
    AuthenticatedUser founder = new AuthenticatedUser(founderId, "subject-1", "Founder");
    CreatePolityInput input =
        new CreatePolityInput("Private Room", PolityVisibility.PRIVATE, null, null);

    when(entitlements.require(founderId, PolityResources.PRODUCT)).thenReturn(entitlement(1));
    when(polities.countByFounderIdAndVisibilityAndStatus(
            founderId, PolityVisibility.PRIVATE, PolityStatus.ACTIVE))
        .thenReturn(1L);

    assertThatThrownBy(() -> workflow.create(founder, input))
        .isInstanceOf(ApiException.class)
        .hasMessage("Private polity entitlement limit has been reached.");

    verify(polities).lockFounderPrivatePolityQuota(founderId);
    verify(identityUsers, never()).get(any());
    verify(polities, never()).saveAndFlush(any());
    verify(grants, never()).stage(any());
  }

  @Test
  void createAllowsPrivatePolityWhenEntitlementHasCapacity() {
    UUID founderId = UUID.randomUUID();
    AuthenticatedUser founder = new AuthenticatedUser(founderId, "subject-1", "Founder");
    CreatePolityInput input =
        new CreatePolityInput("Private Room", PolityVisibility.PRIVATE, null, null);

    when(entitlements.require(founderId, PolityResources.PRODUCT)).thenReturn(entitlement(2));
    when(polities.countByFounderIdAndVisibilityAndStatus(
            founderId, PolityVisibility.PRIVATE, PolityStatus.ACTIVE))
        .thenReturn(1L);
    when(identityUsers.get(founderId))
        .thenThrow(ApiException.of(500, "stop", "Stop after billing branch."));

    assertThatThrownBy(() -> workflow.create(founder, input))
        .isInstanceOf(ApiException.class)
        .hasMessage("Stop after billing branch.");

    verify(polities).lockFounderPrivatePolityQuota(founderId);
    verify(identityUsers).get(founderId);
    verify(polities, never()).saveAndFlush(any());
  }

  @Test
  void createChecksBillingOnlyForPrivatePolities() {
    AuthenticatedUser founder = new AuthenticatedUser(UUID.randomUUID(), "subject-1", "Founder");
    CreatePolityInput input =
        new CreatePolityInput("Public Square", PolityVisibility.PUBLIC, null, null);

    when(identityUsers.get(founder.id()))
        .thenThrow(ApiException.of(500, "stop", "Stop after billing branch."));

    assertThatThrownBy(() -> workflow.create(founder, input))
        .isInstanceOf(ApiException.class)
        .hasMessage("Stop after billing branch.");

    verify(entitlements, never()).require(any(), any());
    verify(polities, never()).lockFounderPrivatePolityQuota(any());
  }

  private BillingEntitlement entitlement(Integer tenantLimit) {
    return new BillingEntitlement(
        UUID.randomUUID(),
        UUID.randomUUID(),
        PolityResources.PRODUCT,
        BillingEntitlementStatus.ACTIVE,
        tenantLimit,
        null,
        null,
        null,
        NOW,
        NOW);
  }
}
