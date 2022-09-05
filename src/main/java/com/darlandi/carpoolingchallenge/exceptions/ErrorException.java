package com.darlandi.carpoolingchallenge.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorException extends Exception {
    Logger logger = LoggerFactory.getLogger(ErrorException.class);
    public ErrorException(Class cls, String msg) {
        logger.error(cls.getName() + " -> Error: " + msg);
    }
}
