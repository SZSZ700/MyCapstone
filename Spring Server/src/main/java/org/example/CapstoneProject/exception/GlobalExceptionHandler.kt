@file:Suppress("PackageName")
package org.example.CapstoneProject.exception
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

// -------------------------------------------------------------------------
// Handles exceptions thrown by REST controllers.
//
// This class provides centralized error responses so controllers do not
// need to handle validation errors individually.
// -------------------------------------------------------------------------
@Suppress("unused")
@RestControllerAdvice
class GlobalExceptionHandler {
    // ---------------------------------------------------------------------
    // Handles validation errors caused by invalid @Valid request bodies.
    //
    // Returns HTTP 400 with a map containing the validation error
    // message for each invalid field.
    // ---------------------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException
    ): ResponseEntity<Map<String, Any?>> {
        // Store validation messages by field name.
        val errors = LinkedHashMap<String, String?>()
        // Read all field validation errors from the exception.
        ex.bindingResult.fieldErrors.forEach { error ->
                // Store the validation message under the invalid field name.
                errors[error.field] = error.defaultMessage
            }

        // Build the response body.
        val response = LinkedHashMap<String, Any?>()
        // Add the validation errors to the response.
        response["errors"] = errors

        // Return HTTP 400 Bad Request.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    // ---------------------------------------------------------------------
    // Handles invalid values detected manually inside the service layer.
    //
    // Returns HTTP 400 with the exception message.
    // ---------------------------------------------------------------------
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException
    ): ResponseEntity<Map<String, Any?>> {
        // Build the response body.
        val response = LinkedHashMap<String, Any?>()
        // Add the exception message to the response.
        response["error"] = ex.message

        // Return HTTP 400 Bad Request.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }
}