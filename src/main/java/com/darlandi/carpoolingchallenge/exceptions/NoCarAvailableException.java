package com.darlandi.carpoolingchallenge.exceptions;

public class NoCarAvailableException extends ErrorException {

    public NoCarAvailableException(Class cls){
        super(cls, "No car with seats available");
    }
}
