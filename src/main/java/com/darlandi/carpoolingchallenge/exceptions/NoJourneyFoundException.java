package com.darlandi.carpoolingchallenge.exceptions;

public class NoJourneyFoundException extends ErrorException {

    public NoJourneyFoundException(Class cls){
        super(cls, "No journey found");
    }
}
