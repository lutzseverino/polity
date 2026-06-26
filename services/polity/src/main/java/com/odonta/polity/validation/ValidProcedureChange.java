package com.odonta.polity.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidProcedureChangeValidator.class)
public @interface ValidProcedureChange {
  String message() default "{polity.validation.procedureChange.validRules}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
