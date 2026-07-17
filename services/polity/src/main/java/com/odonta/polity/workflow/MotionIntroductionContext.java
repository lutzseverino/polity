package com.odonta.polity.workflow;

import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.ConstitutionalMotionPath;
import com.odonta.polity.model.Institution;
import com.odonta.polity.model.Jurisdiction;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Procedure;
import java.time.OffsetDateTime;
import java.util.UUID;

record MotionIntroductionContext(
    UUID polityId,
    Membership introducer,
    ConstitutionVersion constitution,
    Jurisdiction jurisdiction,
    Institution institution,
    Procedure procedure,
    ConstitutionalMotionPath path,
    OffsetDateTime now) {}
