package com.odonta.polity.model;

public record PolityActionAvailabilityResult(
    ActionAvailabilityResult inviteMembers,
    ActionAvailabilityResult introduceMotion,
    ActionAvailabilityResult introduceOfficeElection,
    ActionAvailabilityResult introduceSanction,
    ActionAvailabilityResult introduceAppeal,
    ActionAvailabilityResult introduceOfficeTermReview,
    ActionAvailabilityResult introduceConstitutionalReview,
    ActionAvailabilityResult introduceAmendment,
    ActionAvailabilityResult introduceDisbandment,
    ActionAvailabilityResult requestCertification) {}
