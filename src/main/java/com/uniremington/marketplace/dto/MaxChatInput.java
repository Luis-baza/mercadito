package com.uniremington.marketplace.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxChatInputValidator.class)
public @interface MaxChatInput {

    String message() default "El mensaje supera el máximo de caracteres permitido.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
