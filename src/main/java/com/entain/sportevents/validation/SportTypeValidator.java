package com.entain.sportevents.validation;

import com.entain.sportevents.config.SportTypesConfig;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validates a sport type string against the configured list in {@code application.yml}.
 * Comparison is case-insensitive.
 *
 * <p>When the annotation's {@code required = false}, a {@code null} value is accepted
 * (meaning "no filter"); any non-null value is still checked against the configured list.
 */
public class SportTypeValidator implements ConstraintValidator<ValidSportType, String> {

    @Autowired
    private SportTypesConfig sportTypesConfig;

    private boolean required;

    @Override
    public void initialize(ValidSportType annotation) {
        this.required = annotation.required();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            // null is OK when the field is optional (e.g. query param filter)
            return !required;
        }
        return sportTypesConfig.getTypes().stream()
                .anyMatch(configured -> configured.equalsIgnoreCase(value));
    }
}
