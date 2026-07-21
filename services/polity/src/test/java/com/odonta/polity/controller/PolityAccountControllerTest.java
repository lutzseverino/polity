package com.odonta.polity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odonta.polity.api.model.GrantConvergenceResponse;
import com.odonta.polity.api.model.GrantConvergenceStatus;
import com.odonta.polity.api.model.PolityAccountResponse;
import com.odonta.polity.mapper.PolityAccountTransportMapper;
import com.odonta.polity.mapper.PolityTransportMapper;
import com.odonta.polity.result.GrantConvergenceResult;
import com.odonta.polity.result.PolityAccountResult;
import com.odonta.polity.result.ProvisionPolityAccountResult;
import com.odonta.polity.service.PolityAccountService;
import com.odonta.polity.service.PolityService;
import com.odonta.polity.service.PolitySlugLookupService;
import com.odonta.polity.workflow.CreatePolityWorkflow;
import com.odonta.polity.workflow.ProvisionPolityAccountWorkflow;
import io.github.lutzseverino.cardo.authorization.grant.GrantReceiptStatus;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUserReader;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PolityAccountControllerTest {
  private final PolityAccountService accounts = mock(PolityAccountService.class);
  private final PolityAccountTransportMapper mapper = mock(PolityAccountTransportMapper.class);
  private final ProvisionPolityAccountWorkflow provisioning =
      mock(ProvisionPolityAccountWorkflow.class);
  private final AuthenticatedUserReader users = mock(AuthenticatedUserReader.class);
  private final PolityController controller =
      new PolityController(
          mock(CreatePolityWorkflow.class),
          mock(PolityService.class),
          mock(PolitySlugLookupService.class),
          mock(PolityTransportMapper.class),
          accounts,
          mapper,
          provisioning,
          users);

  @Test
  void newlyProvisionedAccountsReturnTheDurableResultAsCreated() {
    AuthenticatedUser user = user();
    PolityAccountResult result = result(user.id(), GrantReceiptStatus.PENDING, null);
    PolityAccountResponse response = mock(PolityAccountResponse.class);
    when(users.currentUser()).thenReturn(user);
    when(provisioning.provision(user)).thenReturn(new ProvisionPolityAccountResult(result, true));
    when(mapper.toResponse(result)).thenReturn(response);

    var entity = controller.provisionPolityAccount();

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(entity.getBody()).isSameAs(response);
  }

  @Test
  void repeatedProvisioningAndPollingReturnTheCurrentDurableResult() {
    AuthenticatedUser user = user();
    PolityAccountResult result =
        result(user.id(), GrantReceiptStatus.FAILED, "provider_application_failed");
    PolityAccountResponse response = mock(PolityAccountResponse.class);
    when(users.currentUser()).thenReturn(user);
    when(provisioning.provision(user)).thenReturn(new ProvisionPolityAccountResult(result, false));
    when(accounts.get(user.id())).thenReturn(result);
    when(mapper.toResponse(result)).thenReturn(response);

    assertThat(controller.provisionPolityAccount().getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(controller.getPolityAccount().getBody()).isSameAs(response);
  }

  @Test
  void nonFailedConvergenceSerializesItsNullableFailureCode() throws Exception {
    var response = new GrantConvergenceResponse(UUID.randomUUID(), GrantConvergenceStatus.PENDING);

    assertThat(new ObjectMapper().writeValueAsString(response)).contains("\"failureCode\":null");
  }

  private AuthenticatedUser user() {
    return new AuthenticatedUser(UUID.randomUUID(), "account-subject", "Citizen");
  }

  private PolityAccountResult result(UUID userId, GrantReceiptStatus status, String failureCode) {
    return new PolityAccountResult(
        userId, new GrantConvergenceResult(UUID.randomUUID(), status, failureCode));
  }
}
