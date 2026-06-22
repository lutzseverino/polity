package com.odonta.polity.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPowerChangeValidator.class)
public @interface ValidPowerChange {
  String message() default "Power change holder fields must match the holder scope.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
