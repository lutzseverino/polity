package com.odonta.polity.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidConstitutionAmendmentValidator.class)
public @interface ValidConstitutionAmendment {
  String message() default "{polity.validation.constitutionAmendment.validChanges}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
