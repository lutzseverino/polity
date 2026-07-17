package com.odonta.polity.workflow;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.input.CreateMotionInput;
import com.odonta.polity.model.ConstitutionalMotionPath;
import com.odonta.polity.model.Motion;
import com.odonta.polity.result.MotionResult;
import io.github.lutzseverino.cardo.authorization.spring.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@RequiredArgsConstructor
public class IntroduceMotionWorkflow {
  private final ConstitutionalAuthority authority;
  private final MotionIntroducer motions;

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult introduce(
      UUID polityId, AuthenticatedUser actor, @Valid CreateMotionInput input) {
    MotionIntroductionContext context =
        motions.prepare(polityId, actor, ConstitutionalMotionPath.ORDINARY_GOVERNANCE);
    authority.require(
        context.introducer(), context.constitution(), context.path().introducingPower());
    Motion motion = motions.introduce(context, input.title(), input.body(), null, Set.of());
    return motions.result(motion, context.introducer().getId());
  }
}
