package com.odonta.polity.workflow;

import com.odonta.polity.PolityPermissions;
import com.odonta.polity.authorization.ConstitutionalAuthority;
import com.odonta.polity.input.CreateDisbandmentMotionInput;
import com.odonta.polity.model.ConstitutionalMotionPath;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.MotionTemplate;
import com.odonta.polity.model.MotionTemplateKey;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.result.MotionResult;
import com.odonta.polity.service.PolityService;
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
public class IntroduceDisbandmentMotionWorkflow {
  private final ConstitutionalAuthority authority;
  private final MotionIntroducer motions;
  private final PolityService polities;

  @Transactional
  @PreAuthorize(PolityPermissions.HAS_POLITY_PARTICIPATE)
  public MotionResult introduce(
      UUID polityId, AuthenticatedUser actor, @Valid CreateDisbandmentMotionInput input) {
    MotionIntroductionContext context =
        motions.prepare(polityId, actor, ConstitutionalMotionPath.DISBANDMENT);
    authority.require(
        context.introducer(), context.constitution(), context.path().introducingPower());
    polities.requireDisbandmentGovernment(polityId);
    MotionTemplate template =
        MotionTemplate.of(
            MotionTemplateKey.DISBANDMENT,
            TemplateParameters.of("title", input.title(), "body", input.body()));
    Motion motion =
        motions.introduce(
            context, template.storedTitle(), template.storedBody(), template, Set.of());
    return motions.result(motion, context.introducer().getId());
  }
}
