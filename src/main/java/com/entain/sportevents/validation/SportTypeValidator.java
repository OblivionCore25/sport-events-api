package com.entain.sportevents.validation;

import com.entain.sportevents.config.SportTypesConfig;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validates a sport type string against the configured list in {@code application.yml}.
 * Comparison is case-insensitive.
 */
public class SportTypeValidator implements ConstraintValidator<ValidSportType, String> {

    @Autowired
    private SportTypesConfig sportTypesConfig;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return sportTypesConfig.getTypes().stream()
                .anyMatch(configured -> configured.equalsIgnoreCase(value));
    }
}
