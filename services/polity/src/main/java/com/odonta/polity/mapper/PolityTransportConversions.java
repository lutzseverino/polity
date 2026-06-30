package com.odonta.polity.mapper;

import com.odonta.polity.api.model.ConstitutionalHealthDiagnosticResponse;
import com.odonta.polity.api.model.GovernmentReadinessDiagnosticResponse;
import com.odonta.polity.model.ActionAvailabilityResult;
import com.odonta.polity.model.ConstitutionInstitutionResult;
import com.odonta.polity.model.ConstitutionPowerResult;
import com.odonta.polity.model.ConstitutionProcedureResult;
import com.odonta.polity.model.ConstitutionalHealthDiagnostic;
import com.odonta.polity.model.ConstitutionalHealthResult;
import com.odonta.polity.model.GovernmentReadinessDiagnostic;
import com.odonta.polity.model.GovernmentReadinessResult;
import com.odonta.polity.model.PolityResult;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolityTransportConversions {
  private final TransportTextResolver text;

  @Named("summaryInstitutionName")
  public String summaryInstitutionName(PolityResult result) {
    return text.resolveName(result.institutionNameKey(), result.institutionName());
  }

  @Named("institutionName")
  public String institutionName(ConstitutionInstitutionResult result) {
    return text.resolveName(result.nameKey(), result.name());
  }

  @Named("procedureName")
  public String procedureName(ConstitutionProcedureResult result) {
    return text.resolveName(result.nameKey(), result.name());
  }

  @Named("powerName")
  public String powerName(ConstitutionPowerResult result) {
    return text.resolveName(result.nameKey(), result.name());
  }

  @Named("availabilityReasonMessage")
  public String availabilityReasonMessage(ActionAvailabilityResult result) {
    if (result == null || result.reason() == null || result.reason().isBlank()) {
      return null;
    }
    return text.resolveName("api_error." + result.reason(), result.reason());
  }

  @Named("readinessStatusMessage")
  public String readinessStatusMessage(GovernmentReadinessResult result) {
    if (result == null || result.status() == null) {
      return null;
    }
    return text.resolveName(
        key("government_readiness.status", result.status()), result.status().name());
  }

  @Named("readinessDiagnostics")
  public List<GovernmentReadinessDiagnosticResponse> readinessDiagnostics(
      GovernmentReadinessResult result) {
    if (result == null || result.diagnostics() == null) {
      return List.of();
    }
    return result.diagnostics().stream().map(this::readinessDiagnostic).toList();
  }

  @Named("constitutionalHealthStatusMessage")
  public String constitutionalHealthStatusMessage(ConstitutionalHealthResult result) {
    if (result == null || result.status() == null) {
      return null;
    }
    return text.resolveName(
        key("constitutional_health.status", result.status()), result.status().name());
  }

  @Named("constitutionalHealthDiagnostics")
  public List<ConstitutionalHealthDiagnosticResponse> constitutionalHealthDiagnostics(
      ConstitutionalHealthResult result) {
    if (result == null || result.diagnostics() == null) {
      return List.of();
    }
    return result.diagnostics().stream().map(this::constitutionalHealthDiagnostic).toList();
  }

  private GovernmentReadinessDiagnosticResponse readinessDiagnostic(
      GovernmentReadinessDiagnostic diagnostic) {
    String value = diagnostic.name().toLowerCase(Locale.ROOT);
    return new GovernmentReadinessDiagnosticResponse(
        com.odonta.polity.api.model.GovernmentReadinessDiagnostic.fromValue(value),
        text.resolveName("government_readiness.diagnostic." + value, diagnostic.name()));
  }

  private ConstitutionalHealthDiagnosticResponse constitutionalHealthDiagnostic(
      ConstitutionalHealthDiagnostic diagnostic) {
    String value = diagnostic.name().toLowerCase(Locale.ROOT);
    return new ConstitutionalHealthDiagnosticResponse(
        com.odonta.polity.api.model.ConstitutionalHealthDiagnostic.fromValue(value),
        text.resolveName("constitutional_health.diagnostic." + value, diagnostic.name()));
  }

  private String key(String prefix, Enum<?> value) {
    return prefix + "." + value.name().toLowerCase(Locale.ROOT);
  }
}
