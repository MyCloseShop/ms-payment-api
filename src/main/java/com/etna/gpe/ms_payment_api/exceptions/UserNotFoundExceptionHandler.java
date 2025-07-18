package com.etna.gpe.ms_payment_api.exceptions;

import com.etna.gpe.mycloseshop.common_api.ms_login.dto.error.ResponseError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class UserNotFoundExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ResponseError> handleUserNotFoundException(UserNotFoundException e) {
        return ResponseEntity.status(404).body(new ResponseError(
                404,
                "User not found",
                Map.of("details", e.getMessage())
        ));
    }

}
