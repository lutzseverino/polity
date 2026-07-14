package com.odonta.polity.mapper;

import com.odonta.polity.result.ProcedureResult;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcedureTransportText {
  private final TransportTextResolver text;

  @Named("procedureName")
  public String name(ProcedureResult result) {
    return text.resolveName(result.nameKey(), result.name());
  }
}
