package com.odonta.polity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odonta.polity.repository.PolityRepository;
import org.junit.jupiter.api.Test;

class PolitySlugServiceTest {
  private final PolityRepository polities = mock(PolityRepository.class);
  private final PolitySlugService slugs = new PolitySlugService(polities);

  @Test
  void claimsReadableNormalizedSlug() {
    assertThat(slugs.claim("  Peña’s Thursday Assembly  ")).isEqualTo("penas-thursday-assembly");

    verify(polities).lockPolitySlugClaims();
  }

  @Test
  void appendsFirstAvailableSuffixForDuplicateNames() {
    when(polities.existsBySlug("thursday-assembly")).thenReturn(true);
    when(polities.existsBySlug("thursday-assembly-2")).thenReturn(true);

    assertThat(slugs.claim("Thursday Assembly")).isEqualTo("thursday-assembly-3");
  }

  @Test
  void skipsSuffixesAlreadyOwnedByAnotherBaseName() {
    when(polities.existsBySlug("alpha")).thenReturn(true);
    when(polities.existsBySlug("alpha-2")).thenReturn(true);

    assertThat(slugs.claim("Alpha")).isEqualTo("alpha-3");
  }

  @Test
  void keepsStaticPolityRoutesAvailable() {
    assertThat(slugs.claim("New")).isEqualTo("new-polity");
    assertThat(slugs.claim("Membership Invitations")).isEqualTo("membership-invitations-polity");
  }

  @Test
  void fallsBackForNamesWithoutAsciiLettersOrNumbers() {
    assertThat(slugs.claim("共同体")).isEqualTo("polity");
  }
}
