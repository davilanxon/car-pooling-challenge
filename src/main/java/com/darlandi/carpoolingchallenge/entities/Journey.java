package com.darlandi.carpoolingchallenge.entities;

import java.io.Serializable;

/**
 * Journey object to be stored in the Redis DB.
 * The carId is assigned when there is an available car for that journey.
 */
public class Journey implements Serializable {
    private Long id;
    private Integer people;
    private Long carId;
    private int waitingWeight;

    public Journey() {
    }

    public Journey(Long id, Integer people) {
        this.id = id;
        this.people = people;
    }

    public int getWaitingWeight() {
        return waitingWeight;
    }

    public void setWaitingWeight(Integer waitingWeight) {
        this.waitingWeight = waitingWeight;
    }

    public Long getCarId() {
        return carId;
    }

    public void setCarId(Long carId) {
        this.carId = carId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getPeople() {
        return people;
    }

    public void setPeople(Integer people) {
        this.people = people;
    }
}
