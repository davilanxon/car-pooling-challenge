package com.darlandi.carpoolingchallenge.entities;

/**
 * Simplified journey object to not show irrelevant data.
 */
public class JourneyDataTransferObject {
    private Long id;
    private Integer people;

    public JourneyDataTransferObject() {
    }

    public JourneyDataTransferObject(Long id, Integer people) {
        this.id = id;
        this.people = people;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPeople() {
        return people;
    }

    public void setPeople(Integer people) {
        this.people = people;
    }
}
