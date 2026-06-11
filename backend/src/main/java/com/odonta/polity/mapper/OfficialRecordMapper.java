package com.odonta.polity.mapper;

import com.odonta.polity.api.model.OfficialRecordEntryResponse;
import com.odonta.polity.api.model.OfficialRecordResponse;
import com.odonta.polity.model.OfficialRecordDetails;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OfficialRecordMapper {
  public OfficialRecordResponse toResponse(List<OfficialRecordDetails> records) {
    return new OfficialRecordResponse(records.stream().map(this::toResponse).toList());
  }

  private OfficialRecordEntryResponse toResponse(OfficialRecordDetails details) {
    return new OfficialRecordEntryResponse(
            details.entry().getId(),
            details.entry().getType().name().toLowerCase(),
            details.entry().getTitle(),
            details.entry().getBody(),
            details.actor().getDisplayName(),
            details.constitution().getVersion(),
            details.entry().getOccurredAt())
        .sourceId(details.entry().getSourceId());
  }
}
