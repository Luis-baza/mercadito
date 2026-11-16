package com.uniremington.marketplace.service;

import org.springframework.http.HttpStatus;

public class GeminiChatException extends RuntimeException {

    private final HttpStatus status;

    public GeminiChatException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
