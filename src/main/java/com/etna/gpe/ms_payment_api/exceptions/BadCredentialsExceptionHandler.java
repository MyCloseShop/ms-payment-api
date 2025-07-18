package com.etna.gpe.ms_payment_api.exceptions;

import com.etna.gpe.mycloseshop.common_api.ms_login.dto.error.ResponseError;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class BadCredentialsExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ResponseError> handleBadCredentialsException(BadCredentialsException e) {
        return ResponseEntity.status(401).body(new ResponseError(
                401,
                "Bad credentials",
                Map.of("details", e.getMessage())
        ));
    }
}
