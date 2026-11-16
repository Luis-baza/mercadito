package com.uniremington.marketplace.dto;

import com.uniremington.marketplace.config.GeminiProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

@Component
public class MaxChatInputValidator implements ConstraintValidator<MaxChatInput, String> {

    private final GeminiProperties properties;

    public MaxChatInputValidator(GeminiProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.length() <= properties.maxInputChars();
    }
}
