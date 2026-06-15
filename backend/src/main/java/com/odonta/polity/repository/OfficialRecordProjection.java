package com.odonta.polity.repository;

import com.odonta.polity.model.OfficialRecordType;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface OfficialRecordProjection {
  UUID getId();

  OfficialRecordType getType();

  String getTitle();

  String getBody();

  String getActorName();

  int getConstitutionVersion();

  UUID getSourceId();

  OffsetDateTime getOccurredAt();
}
