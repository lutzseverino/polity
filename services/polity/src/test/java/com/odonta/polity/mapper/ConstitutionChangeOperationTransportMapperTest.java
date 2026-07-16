package com.odonta.polity.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.odonta.polity.model.ConstitutionChangeOperation;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ConstitutionChangeOperationTransportMapperTest {
  private final MotionTransportMapper mapper = Mappers.getMapper(MotionTransportMapper.class);

  @Test
  void preservesPersistedOperationNames() {
    assertThat(ConstitutionChangeOperation.CREATE.name()).isEqualTo("CREATE");
    assertThat(ConstitutionChangeOperation.REVISE.name()).isEqualTo("REVISE");
    assertThat(ConstitutionChangeOperation.RETIRE.name()).isEqualTo("RETIRE");
  }

  @Test
  void mapsInstitutionActionsAtTheTransportBoundary() {
    assertThat(
            mapper.toOperation(
                com.odonta.polity.api.model.ConstitutionInstitutionChangeAction.CREATE))
        .isEqualTo(ConstitutionChangeOperation.CREATE);
    assertThat(mapper.toInstitutionChangeAction(ConstitutionChangeOperation.REVISE))
        .isEqualTo(com.odonta.polity.api.model.ConstitutionInstitutionChangeAction.REVISE);
    assertThat(mapper.toInstitutionChangeAction(ConstitutionChangeOperation.RETIRE))
        .isEqualTo(com.odonta.polity.api.model.ConstitutionInstitutionChangeAction.RETIRE);
  }

  @Test
  void mapsOfficeActionsAtTheTransportBoundary() {
    assertThat(
            mapper.toOperation(com.odonta.polity.api.model.ConstitutionOfficeChangeAction.CREATE))
        .isEqualTo(ConstitutionChangeOperation.CREATE);
    assertThat(mapper.toOfficeChangeAction(ConstitutionChangeOperation.REVISE))
        .isEqualTo(com.odonta.polity.api.model.ConstitutionOfficeChangeAction.REVISE);
    assertThat(mapper.toOfficeChangeAction(ConstitutionChangeOperation.RETIRE))
        .isEqualTo(com.odonta.polity.api.model.ConstitutionOfficeChangeAction.RETIRE);
  }
}
