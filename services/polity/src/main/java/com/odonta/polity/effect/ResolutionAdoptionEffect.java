package com.odonta.polity.effect;

import com.odonta.polity.model.ConstitutionVersion;
import com.odonta.polity.model.EffectType;
import com.odonta.polity.model.Membership;
import com.odonta.polity.model.Motion;
import com.odonta.polity.model.OfficialRecordContext;
import com.odonta.polity.model.OfficialRecordOutcome;
import com.odonta.polity.model.OfficialRecordTemplate;
import com.odonta.polity.model.OfficialRecordTemplateKey;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.Resolution;
import com.odonta.polity.model.TemplateParameters;
import com.odonta.polity.repository.ResolutionRepository;
import com.odonta.polity.service.OfficialRecordService;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
final class ResolutionAdoptionEffect implements MotionEffect {
  private final ResolutionRepository resolutions;
  private final OfficialRecordService officialRecords;

  @Override
  public EffectType type() {
    return EffectType.ADOPT_RESOLUTION;
  }

  @Override
  public void apply(
      Motion motion, Membership actor, ConstitutionVersion constitution, OffsetDateTime now) {
    Resolution resolution =
        resolutions.saveAndFlush(
            new Resolution(
                motion.getPolityId(), motion.getId(), motion.getTitle(), motion.getBody(), now));
    officialRecords.append(
        motion.getPolityId(),
        motion.getJurisdictionId(),
        constitution.getId(),
        actor.getId(),
        OfficialRecordType.RESOLUTION_ADOPTED,
        resolution.getId(),
        OfficialRecordContext.effect(motion, OfficialRecordOutcome.ADOPTED),
        OfficialRecordTemplate.of(
            OfficialRecordTemplateKey.RESOLUTION_ADOPTED,
            TemplateParameters.with(
                motion.getTemplateParams(),
                "motionTitle",
                motion.getTitle(),
                "motionTitleKey",
                motion.getTitleKey(),
                "motionBody",
                motion.getBody(),
                "motionBodyKey",
                motion.getBodyKey())),
        now);
  }
}
