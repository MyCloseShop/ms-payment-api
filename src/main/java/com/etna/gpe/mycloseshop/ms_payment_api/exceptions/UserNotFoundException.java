package com.etna.gpe.mycloseshop.ms_payment_api.exceptions;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
