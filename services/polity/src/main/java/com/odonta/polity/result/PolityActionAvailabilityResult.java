package com.odonta.polity.result;

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
