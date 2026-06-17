package com.odonta.polity.model;

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
  String message() default "Procedure change must contain a valid set of rule updates.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
