package com.odonta.polity.mapper;

import com.odonta.polity.api.model.ConstitutionAmendmentProposalResponse;
import com.odonta.polity.api.model.ConstitutionInstitutionChangeAction;
import com.odonta.polity.api.model.ConstitutionOfficeChangeAction;
import com.odonta.polity.api.model.CreateConstitutionAmendmentMotionRequest;
import com.odonta.polity.api.model.CreateInstitutionChangeRequest;
import com.odonta.polity.api.model.CreateOfficeChangeRequest;
import com.odonta.polity.api.model.CreatePowerChangeRequest;
import com.odonta.polity.api.model.CreateProcedureChangeRequest;
import com.odonta.polity.input.CreateConstitutionAmendmentMotionInput;
import com.odonta.polity.input.CreateInstitutionChangeInput;
import com.odonta.polity.input.CreateOfficeChangeInput;
import com.odonta.polity.input.CreatePowerChangeInput;
import com.odonta.polity.input.CreateProcedureChangeInput;
import com.odonta.polity.model.ConstitutionChangeOperation;
import com.odonta.polity.result.ConstitutionAmendmentProposalResult;
import org.mapstruct.Mapper;

@Mapper(config = PolityMapperConfig.class)
public interface ConstitutionAmendmentTransportMapper {
  CreateConstitutionAmendmentMotionInput toInput(CreateConstitutionAmendmentMotionRequest request);

  CreateInstitutionChangeInput toInput(CreateInstitutionChangeRequest request);

  CreateProcedureChangeInput toInput(CreateProcedureChangeRequest request);

  CreateOfficeChangeInput toInput(CreateOfficeChangeRequest request);

  CreatePowerChangeInput toInput(CreatePowerChangeRequest request);

  ConstitutionAmendmentProposalResponse toResponse(ConstitutionAmendmentProposalResult result);

  default ConstitutionChangeOperation toOperation(ConstitutionInstitutionChangeAction action) {
    if (action == null) {
      return null;
    }
    return switch (action) {
      case CREATE -> ConstitutionChangeOperation.CREATE;
      case RETIRE -> ConstitutionChangeOperation.RETIRE;
      case REVISE -> ConstitutionChangeOperation.REVISE;
    };
  }

  default ConstitutionChangeOperation toOperation(ConstitutionOfficeChangeAction action) {
    if (action == null) {
      return null;
    }
    return switch (action) {
      case CREATE -> ConstitutionChangeOperation.CREATE;
      case RETIRE -> ConstitutionChangeOperation.RETIRE;
      case REVISE -> ConstitutionChangeOperation.REVISE;
    };
  }

  default ConstitutionInstitutionChangeAction toInstitutionChangeAction(
      ConstitutionChangeOperation operation) {
    if (operation == null) {
      return null;
    }
    return switch (operation) {
      case CREATE -> ConstitutionInstitutionChangeAction.CREATE;
      case RETIRE -> ConstitutionInstitutionChangeAction.RETIRE;
      case REVISE -> ConstitutionInstitutionChangeAction.REVISE;
    };
  }

  default ConstitutionOfficeChangeAction toOfficeChangeAction(
      ConstitutionChangeOperation operation) {
    if (operation == null) {
      return null;
    }
    return switch (operation) {
      case CREATE -> ConstitutionOfficeChangeAction.CREATE;
      case RETIRE -> ConstitutionOfficeChangeAction.RETIRE;
      case REVISE -> ConstitutionOfficeChangeAction.REVISE;
    };
  }
}
