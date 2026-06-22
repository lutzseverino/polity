package com.odonta.polity.authorization;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.odonta.polity.model.PolityVisibility;
import com.odonta.polity.repository.PolityRepository;
import com.odonta.polity.service.MembershipService;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PolityAccessPolicyTest {
  private final MembershipService memberships = mock(MembershipService.class);
  private final PolityRepository polities = mock(PolityRepository.class);
  private final PolityAccessPolicy access = new PolityAccessPolicy(memberships, polities);

  @Test
  void publicPolityReadDoesNotRequireMembership() {
    UUID polityId = UUID.randomUUID();

    when(polities.existsByIdAndVisibility(polityId, PolityVisibility.PUBLIC)).thenReturn(true);

    access.requireReadable(polityId, UUID.randomUUID());

    verifyNoInteractions(memberships);
  }

  @Test
  void privatePolityReadRequiresActiveMembership() {
    UUID polityId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(polities.existsByIdAndVisibility(polityId, PolityVisibility.PUBLIC)).thenReturn(false);

    access.requireReadable(polityId, userId);

    verify(memberships).requireActive(polityId, userId);
  }
}
