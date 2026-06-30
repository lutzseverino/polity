package com.odonta.polity.repository;

import com.odonta.polity.model.OfficeElectionMethod;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.VotingThreshold;
import java.util.UUID;

public interface ConstitutionProcedureChangeProposalProjection {
  String getProcedureCode();

  UUID getInstitutionId();

  Integer getQuorumNumerator();

  Integer getQuorumDenominator();

  VotingThreshold getThreshold();

  OfficeElectionMethod getOfficeElectionMethod();

  ProcedureElectorate getElectorate();

  String getElectorateOfficeCode();

  Integer getMinimumElectorCount();

  Integer getMinimumNoticeHours();

  Integer getVotingPeriodHours();
}
