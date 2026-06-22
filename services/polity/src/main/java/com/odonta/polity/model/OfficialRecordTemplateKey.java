package com.odonta.polity.model;

import java.util.Map;

public enum OfficialRecordTemplateKey {
  APPEAL_GRANTED(
      "official_record.appeal_granted",
      "Appeal granted for {memberName}",
      "The sanction was vacated after appeal: {reason}"),
  BOOTSTRAP_STEWARD_ASSIGNED(
      "official_record.bootstrap_steward_assigned",
      "{memberName} assigned as {officeName}",
      "The founding citizen received the initial {termLengthDays}-day {officeName} term under {constitutionName}."),
  CANDIDACY_ACCEPTED(
      "official_record.candidacy_accepted",
      "Candidacy accepted for {motionTitle}",
      "{candidateName} accepted the office election nomination."),
  CANDIDACY_DECLINED(
      "official_record.candidacy_declined",
      "Candidacy declined for {motionTitle}",
      "{candidateName} declined the office election nomination."),
  CONSTITUTION_AMENDED(
      "official_record.constitution_amended",
      "Constitution amended to v{constitutionVersion}",
      "{amendmentBody}\n\nChanges: {changeSummary}"),
  CONSTITUTION_RATIFIED(
      "official_record.constitution_ratified", "{constitutionName} ratified", "{constitutionBody}"),
  MEMBER_ADMITTED(
      "official_record.member_admitted",
      "{memberName} accepted membership",
      "{memberName} accepted the pending invitation and became an active citizen."),
  MEMBER_INVITED(
      "official_record.member_invited",
      "{inviteeEmail} was invited",
      "{inviterName} invited {inviteeEmail} to become a citizen."),
  MOTION_CERTIFIED(
      "official_record.motion_certified", "Result certified: {motionTitle}", "{outcomeReason}"),
  MOTION_INTRODUCED(
      "official_record.motion_introduced",
      "{motionTitle}",
      "{introducerName} introduced {procedureName} for the {institutionName} under Constitution v{constitutionVersion}. Voting opens at {votingOpensAt} with {eligibleElectorCount} eligible electors."),
  MOTION_REJECTED(
      "official_record.motion_rejected",
      "Motion rejected: {motionTitle}",
      "The certified result did not authorize the proposed effect."),
  OFFICE_ASSIGNED(
      "official_record.office_assigned",
      "{memberName} assigned as {officeName}",
      "{memberName} was assigned to the office of {officeName} for {termLengthDays} days."),
  OFFICE_ELECTED(
      "official_record.office_elected",
      "{memberName} elected as {officeName}",
      "{memberName} was elected to the office of {officeName} for {termLengthDays} days."),
  OFFICE_ELECTION_BALLOT_CAST(
      "official_record.office_election_ballot_cast",
      "Election ballot recorded on {motionTitle}",
      "{voterName} cast or updated an election ballot while voting was open."),
  POLITY_DISBANDED(
      "official_record.polity_disbanded", "Polity disbanded: {polityName}", "{motionBody}"),
  POLITY_FOUNDED(
      "official_record.polity_founded",
      "{polityName} was founded",
      "The polity was founded with the {setupPreset} preset and {pace} pace."),
  RESOLUTION_ADOPTED(
      "official_record.resolution_adopted", "Resolution adopted: {motionTitle}", "{motionBody}"),
  SANCTION_APPLIED(
      "official_record.sanction_applied",
      "Sanction applied to {memberName}",
      "{memberName} received a {sanctionType} sanction for {durationDays} days: {reason}"),
  VOTE_CAST(
      "official_record.vote_cast",
      "Vote recorded on {motionTitle}",
      "{voterName} cast or updated a ballot while voting was open.");

  private final String keyPrefix;
  private final String fallbackTitle;
  private final String fallbackBody;

  OfficialRecordTemplateKey(String keyPrefix, String fallbackTitle, String fallbackBody) {
    this.keyPrefix = keyPrefix;
    this.fallbackTitle = fallbackTitle;
    this.fallbackBody = fallbackBody;
  }

  public String titleKey() {
    return keyPrefix + ".title";
  }

  public String bodyKey() {
    return keyPrefix + ".body";
  }

  public String fallbackTitle(Map<String, ?> params) {
    return TemplateText.render(fallbackTitle, params);
  }

  public String fallbackBody(Map<String, ?> params) {
    return TemplateText.render(fallbackBody, params);
  }
}
