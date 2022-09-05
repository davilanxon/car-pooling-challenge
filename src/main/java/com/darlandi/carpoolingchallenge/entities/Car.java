package com.darlandi.carpoolingchallenge.entities;

import org.springframework.data.redis.core.RedisHash;

import javax.persistence.Id;
import java.io.Serializable;

/**
 * Car object to be stored in the Redis DB.
 * AvailableSeats is necessary to know the people who can travel in the car.
 */
public class Car implements Serializable {
    private Long id;
    private Integer seats;
    private Integer availableSeats;

    public Car() {
    }

    public Car(Long id, Integer totalSeats) {
        this.id = id;
        this.seats = totalSeats;
        this.availableSeats = totalSeats;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSeats() {
        return seats;
    }

    public void setSeats(Integer seats) {
        this.seats = seats;
    }
}

