package com.darlandi.carpoolingchallenge.entities;

/**
 * Simplified car object to not show irrelevant data.
 */
public class CarDataTransferObject {
    private Long id;
    private Integer seats;

    public CarDataTransferObject() {
    }

    public CarDataTransferObject(Long id, Integer seats) {
        this.id = id;
        this.seats = seats;
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
