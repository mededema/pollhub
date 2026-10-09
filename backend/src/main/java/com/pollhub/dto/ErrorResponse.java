package com.pollhub.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

// fieldErrors n'apparait dans le JSON que pour les erreurs de validation
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now().toString(), status, error, message, path, null);
    }

    public static ErrorResponse of(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now().toString(), status, error, message, path, fieldErrors);
    }
}