package com.odonta.polity.mapper;

import com.odonta.polity.result.MotionResult;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MotionTransportText {
  private final TransportTextResolver text;

  @Named("motionTitle")
  public String title(MotionResult result) {
    return text.resolve(result.titleKey(), result.title(), result.templateParams());
  }

  @Named("motionBody")
  public String body(MotionResult result) {
    return text.resolve(result.bodyKey(), result.body(), result.templateParams());
  }

  @Named("motionProcedureName")
  public String procedureName(MotionResult result) {
    return text.resolveName(result.procedureNameKey(), result.procedureName());
  }
}
