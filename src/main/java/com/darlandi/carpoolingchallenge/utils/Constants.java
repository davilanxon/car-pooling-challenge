package com.darlandi.carpoolingchallenge.utils;

import org.springframework.stereotype.Component;

/**
 * Max and min limits of seats and people.
 */
@Component
public final class Constants {
    public static final int MAX_SEATS = 6;
    public static final int MIN_SEATS = 4;
    public static final int MAX_PEOPLE = 6;
    public static final int MIN_PEOPLE = 1;

    private Constants() {
    }
}
