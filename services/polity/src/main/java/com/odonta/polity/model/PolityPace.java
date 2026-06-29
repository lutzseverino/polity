package com.odonta.polity.model;

public enum PolityPace {
  FAST(7, 24, 6, 24, 6, 24, 12, 72),
  STANDARD(14, 48, 12, 48, 12, 48, 24, 120),
  DELIBERATE(30, 96, 24, 96, 24, 96, 48, 168);

  private final int starterOfficeTermDays;
  private final int ordinaryVotingPeriodHours;
  private final int officeElectionMinimumNoticeHours;
  private final int officeElectionVotingPeriodHours;
  private final int sanctionMinimumNoticeHours;
  private final int sanctionVotingPeriodHours;
  private final int constitutionalAmendmentMinimumNoticeHours;
  private final int constitutionalAmendmentVotingPeriodHours;

  PolityPace(
      int starterOfficeTermDays,
      int ordinaryVotingPeriodHours,
      int officeElectionMinimumNoticeHours,
      int officeElectionVotingPeriodHours,
      int sanctionMinimumNoticeHours,
      int sanctionVotingPeriodHours,
      int constitutionalAmendmentMinimumNoticeHours,
      int constitutionalAmendmentVotingPeriodHours) {
    this.starterOfficeTermDays = starterOfficeTermDays;
    this.ordinaryVotingPeriodHours = ordinaryVotingPeriodHours;
    this.officeElectionMinimumNoticeHours = officeElectionMinimumNoticeHours;
    this.officeElectionVotingPeriodHours = officeElectionVotingPeriodHours;
    this.sanctionMinimumNoticeHours = sanctionMinimumNoticeHours;
    this.sanctionVotingPeriodHours = sanctionVotingPeriodHours;
    this.constitutionalAmendmentMinimumNoticeHours = constitutionalAmendmentMinimumNoticeHours;
    this.constitutionalAmendmentVotingPeriodHours = constitutionalAmendmentVotingPeriodHours;
  }

  public int starterOfficeTermDays() {
    return starterOfficeTermDays;
  }

  public int ordinaryVotingPeriodHours() {
    return ordinaryVotingPeriodHours;
  }

  public int officeElectionMinimumNoticeHours() {
    return officeElectionMinimumNoticeHours;
  }

  public int officeElectionVotingPeriodHours() {
    return officeElectionVotingPeriodHours;
  }

  public int sanctionMinimumNoticeHours() {
    return sanctionMinimumNoticeHours;
  }

  public int sanctionVotingPeriodHours() {
    return sanctionVotingPeriodHours;
  }

  public int constitutionalAmendmentMinimumNoticeHours() {
    return constitutionalAmendmentMinimumNoticeHours;
  }

  public int constitutionalAmendmentVotingPeriodHours() {
    return constitutionalAmendmentVotingPeriodHours;
  }

  public String labelKey() {
    return "polity.pace." + name().toLowerCase();
  }
}
