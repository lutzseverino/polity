package com.odonta.polity.effect;

import com.odonta.polity.model.OfficialRecordEntry;
import com.odonta.polity.model.OfficialRecordType;
import com.odonta.polity.model.ResolutionStatus;
import com.odonta.polity.repository.OfficeTermRepository;
import com.odonta.polity.repository.ResolutionRepository;
import com.odonta.polity.repository.SanctionRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfficialActVoidRemedy {
  private final OfficeTermRepository officeTerms;
  private final ResolutionRepository resolutions;
  private final SanctionRepository sanctions;

  public boolean hasActiveRemedy(OfficialRecordEntry target, OffsetDateTime now) {
    if (target.getType() == OfficialRecordType.RESOLUTION_ADOPTED) {
      return resolutions
          .findEntityByIdAndPolityId(target.getSourceId(), target.getPolityId())
          .filter(resolution -> resolution.getStatus() == ResolutionStatus.ADOPTED)
          .isPresent();
    }
    if (target.getType() == OfficialRecordType.SANCTION_APPLIED) {
      return sanctions
          .findEntityByIdAndPolityId(target.getSourceId(), target.getPolityId())
          .filter(sanction -> sanction.isActiveAt(now))
          .isPresent();
    }
    if (target.getType() == OfficialRecordType.OFFICE_ELECTED) {
      return officeTerms
          .findEntityByIdAndPolityId(target.getSourceId(), target.getPolityId())
          .filter(term -> term.isActiveAt(now))
          .isPresent();
    }
    return false;
  }

  public boolean apply(OfficialRecordEntry target, OffsetDateTime now) {
    if (target.getType() == OfficialRecordType.RESOLUTION_ADOPTED) {
      return resolutions
          .findEntityByIdAndPolityId(target.getSourceId(), target.getPolityId())
          .filter(resolution -> resolution.getStatus() == ResolutionStatus.ADOPTED)
          .map(
              resolution -> {
                resolution.voidAt(now);
                resolutions.saveAndFlush(resolution);
                return true;
              })
          .orElse(false);
    }
    if (target.getType() == OfficialRecordType.SANCTION_APPLIED) {
      return sanctions
          .findEntityByIdAndPolityId(target.getSourceId(), target.getPolityId())
          .filter(sanction -> sanction.isActiveAt(now))
          .map(
              sanction -> {
                sanction.vacate(now);
                sanctions.saveAndFlush(sanction);
                return true;
              })
          .orElse(false);
    }
    if (target.getType() == OfficialRecordType.OFFICE_ELECTED) {
      return officeTerms
          .findEntityByIdAndPolityId(target.getSourceId(), target.getPolityId())
          .filter(term -> term.isActiveAt(now))
          .map(
              term -> {
                term.end(now);
                officeTerms.saveAndFlush(term);
                return true;
              })
          .orElse(false);
    }
    return false;
  }
}
