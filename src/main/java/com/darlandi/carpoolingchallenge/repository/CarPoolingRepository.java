package com.darlandi.carpoolingchallenge.repository;

import com.darlandi.carpoolingchallenge.entities.Car;
import com.darlandi.carpoolingchallenge.utils.Constants;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository to store cars.
 * Save them into sub-lists depending on the available seats.
 */
@Repository
public class CarPoolingRepository {
    private final HashOperations hashOperations;


    public CarPoolingRepository(RedisTemplate redisTemplate) {
        this.hashOperations = redisTemplate.opsForHash();
    }

    /**
     * Save a car in Redis DB.
     *
     * @param seatsAvailable Number of seats available.
     * @param car Car Object.
     */
    public void create(int seatsAvailable, Car car) {

        hashOperations.putIfAbsent(seatsAvailable, car.getId(), car);
    }

    /**
     * Update a car depending on its new available seats.
     * Delete the car from the old list, and save it in the new list.
     *
     * @param availableSeats Number of seats available.
     * @param car Car object.
     */
    public void update(int availableSeats, Car car) {
        hashOperations.delete(car.getAvailableSeats(), car.getId());
        car.setAvailableSeats(availableSeats);
        hashOperations.put(availableSeats, car.getId(), car);
    }

    /**
     * Return the first car with the required seats available.
     *
     * @param seatsAvailable Number of seats available.
     * @return Optional Car.
     */
    public Optional<Car> getCarSeatsAvailable(int seatsAvailable) {
        try {
            Car car = (Car) hashOperations.values(seatsAvailable).get(0);
            return Optional.of(car);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Search and get the required car given its id.
     *
     * @param carId ID of the car.
     * @return Optional Car.
     */
    public Optional<Car> get(Long carId) {
        if (carId != null) {
            for (int i = 0; i <= Constants.MAX_SEATS; i++) {
                Car car = (Car) hashOperations.get(i, carId);
                if (car != null) {
                    return Optional.of(car);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Get a list of all the cars depending on their seats available.
     *
     * @param seatsAvailable Number of seats available.
     * @return List Car
     */
    public List<Car> getAll(int seatsAvailable) {
        return (List<Car>) hashOperations.values(seatsAvailable);
    }

    /**
     * Clear all the cars stored in Redis DB
     */
    public void deleteAll() {
        for (int seats = 0; seats <= Constants.MAX_SEATS; seats++) {
            if (hashOperations.size(seats) > 0) {
                Set<Long> carIdSet = hashOperations.keys(seats);
                for (Long carId : carIdSet) {
                    hashOperations.delete(seats, carId);
                }
            }
        }
    }

    /**
     * Number of cars stored in the Redis DB.
     * @return Number of cars.
     */
    public long totalSize(){
        long size = 0;
        for (int seats = 0; seats <= Constants.MAX_SEATS; seats++){
            size += hashOperations.size(seats);
        }
        return size;
    }
}
