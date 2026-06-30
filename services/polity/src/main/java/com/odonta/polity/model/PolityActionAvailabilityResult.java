package com.odonta.polity.model;

public record PolityActionAvailabilityResult(
    GovernmentReadinessResult readiness,
    ConstitutionalHealthResult constitutionalHealth,
    ActionAvailabilityResult inviteMembers,
    ActionAvailabilityResult introduceMotion,
    ActionAvailabilityResult introduceOfficeElection,
    ActionAvailabilityResult introduceSanction,
    ActionAvailabilityResult introduceAppeal,
    ActionAvailabilityResult introduceOfficeTermReview,
    ActionAvailabilityResult introduceConstitutionalReview,
    ActionAvailabilityResult introduceAmendment,
    ActionAvailabilityResult introduceDisbandment,
    ActionAvailabilityResult requestCertification,
    ActionAvailabilityResult resignMembership) {}
