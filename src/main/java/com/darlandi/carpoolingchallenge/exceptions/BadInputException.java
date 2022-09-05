package com.darlandi.carpoolingchallenge.exceptions;

public class BadInputException extends ErrorException {
    public BadInputException(Class cls) {
        super(cls, "Bad input");
    }
}
