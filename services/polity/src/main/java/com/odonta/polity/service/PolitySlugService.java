package com.odonta.polity.service;

import com.odonta.polity.repository.PolityRepository;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PolitySlugService {
  public static final int MAX_LENGTH = 80;
  private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
  private static final Pattern APOSTROPHES = Pattern.compile("['’]");
  private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
  private static final Pattern EDGE_HYPHENS = Pattern.compile("(^-+|-+$)");
  private static final Set<String> RESERVED =
      Set.of("invitations", "membership-invitations", "new");

  private final PolityRepository polities;

  public String claim(String name) {
    String base = base(name);
    polities.lockPolitySlugClaims();
    if (!polities.existsBySlug(base)) {
      return base;
    }
    for (int suffix = 2; ; suffix++) {
      String suffixText = "-" + suffix;
      String candidate = truncate(base, MAX_LENGTH - suffixText.length()) + suffixText;
      if (!polities.existsBySlug(candidate)) {
        return candidate;
      }
    }
  }

  static String base(String name) {
    String normalized = Normalizer.normalize(name, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT);
    String slug =
        EDGE_HYPHENS
            .matcher(
                NON_ALPHANUMERIC
                    .matcher(
                        APOSTROPHES
                            .matcher(COMBINING_MARKS.matcher(normalized).replaceAll(""))
                            .replaceAll(""))
                    .replaceAll("-"))
            .replaceAll("");
    slug = truncate(slug.isBlank() ? "polity" : slug, MAX_LENGTH);
    return RESERVED.contains(slug)
        ? truncate(slug, MAX_LENGTH - "-polity".length()) + "-polity"
        : slug;
  }

  private static String truncate(String value, int maximumLength) {
    String truncated = value.substring(0, Math.min(value.length(), maximumLength));
    return EDGE_HYPHENS.matcher(truncated).replaceAll("");
  }
}
