package com.etna.gpe.ms_payment_api.exceptions;

import com.etna.gpe.mycloseshop.common_api.ms_login.dto.error.ResponseError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseError> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        errors.put("status", 400);
        errors.put("errors", new HashMap<>());
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            ((Map<String, Object>) errors.get("errors")).put(fieldName, errorMessage);
        });
        ResponseError responseError = new ResponseError(
                HttpStatus.BAD_REQUEST.value(),
                "Validation error",
                errors
        );
        return ResponseEntity.badRequest().body(responseError);
    }
}
