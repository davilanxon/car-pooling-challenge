package com.darlandi.carpoolingchallenge.exceptions;

public class NoCarFoundException extends ErrorException{
    public NoCarFoundException(Class cls) {
        super(cls, "No car found in the DB");
    }
}

