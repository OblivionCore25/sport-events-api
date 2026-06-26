package com.entain.sportevents.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a sport type value exists in the configured {@code sport.types} list.
 * Case-insensitive comparison is used so "football" and "FOOTBALL" are both accepted.
 *
 * <p>Set {@code required = false} on optional query parameters to allow {@code null} values
 * (i.e. "no filter") while still rejecting unknown non-null sport strings.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SportTypeValidator.class)
public @interface ValidSportType {

    String message() default "Invalid sport type. Check GET /api/sports for the list of valid types.";

    /** When {@code false}, {@code null} values are considered valid (useful for optional filters). */
    boolean required() default true;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
