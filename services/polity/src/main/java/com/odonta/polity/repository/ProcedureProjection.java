package com.odonta.polity.repository;

import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.OfficeElectionMethod;
import com.odonta.polity.model.ProcedureElectorate;
import com.odonta.polity.model.VotingProcedure;
import com.odonta.polity.model.VotingThreshold;
import java.util.UUID;

public interface ProcedureProjection extends VotingProcedure {
  UUID getId();

  UUID getInstitutionId();

  String getCode();

  String getName();

  String getNameKey();

  @Override
  int getQuorumNumerator();

  @Override
  int getQuorumDenominator();

  @Override
  VotingThreshold getThreshold();

  OfficeElectionMethod getOfficeElectionMethod();

  ProcedureElectorate getElectorate();

  String getElectorateOfficeCode();

  int getMinimumElectorCount();

  int getMinimumNoticeHours();

  int getVotingPeriodHours();

  EffectType getEffectType();
}
