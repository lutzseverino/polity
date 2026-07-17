package com.odonta.polity.workflow;

import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.Procedure;
import com.odonta.polity.repository.MembershipRepository;
import com.odonta.polity.repository.MotionElectorRepository;
import com.odonta.polity.repository.MotionProjection;
import com.odonta.polity.repository.MotionRepository;
import com.odonta.polity.repository.VoteRepository;
import com.odonta.polity.service.MotionService;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Narrow test support for exercising the motion read service outside the workflow package. */
public final class MotionReadTestFixture extends MotionWorkflowTestFixture {
  public MotionReadTestFixture() {
    setUp();
  }

  public OffsetDateTime now() {
    return NOW;
  }

  public MotionService service() {
    return queryService;
  }

  public MembershipRepository memberships() {
    return memberships;
  }

  public MotionRepository motions() {
    return motions;
  }

  public MotionElectorRepository electors() {
    return electors;
  }

  public VoteRepository votes() {
    return votes;
  }

  public Membership member(UUID polityId, UUID userId, UUID membershipId) {
    return super.member(polityId, userId, membershipId);
  }

  public Motion motion(UUID polityId, UUID motionId, UUID introducedBy) {
    return super.motion(polityId, motionId, introducedBy);
  }

  public Procedure procedure(UUID polityId, UUID procedureId, UUID constitutionId) {
    return super.procedure(polityId, procedureId, constitutionId);
  }

  public MotionProjection projection(Motion motion, Procedure procedure) {
    return super.projection(motion, procedure);
  }
}
