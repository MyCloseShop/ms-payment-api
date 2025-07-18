package com.etna.gpe.ms_payment_api.exceptions;

import com.etna.gpe.mycloseshop.common_api.ms_login.dto.error.ResponseError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class UserAlreadyExistExceptionHandler {
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseError> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        ResponseError responseError = new ResponseError(
                HttpStatus.CONFLICT.value(),
                "User already exist",
                new HashMap<>(Map.of("message", e.getMessage()))
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                responseError
        );
    }
}
