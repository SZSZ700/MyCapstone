package org.example.CapstoneProject.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

// -------------------------------------------------------------------------
// Handles exceptions thrown by REST controllers.
//
// This class provides centralized error responses so controllers do not
// need to handle validation errors individually.
// -------------------------------------------------------------------------
@SuppressWarnings("unused")
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------------------------------------------------------------------
    // Handles validation errors caused by invalid @Valid request bodies.
    //
    // Returns HTTP 400 with a map containing the validation error
    // message for each invalid field.
    // ---------------------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        // Store validation messages by field name.
        Map<String, String> errors = new LinkedHashMap<>();

        // Read all field validation errors from the exception.
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        // Build the response body.
        Map<String, Object> response = new LinkedHashMap<>();

        // Add the validation errors to the response.
        response.put("errors", errors);

        // Return HTTP 400 Bad Request.
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
}