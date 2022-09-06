package com.darlandi.carpoolingchallenge.services;

import com.darlandi.carpoolingchallenge.entities.Car;
import com.darlandi.carpoolingchallenge.entities.Journey;
import com.darlandi.carpoolingchallenge.exceptions.NoCarAvailableException;
import com.darlandi.carpoolingchallenge.repository.CarPoolingRepository;
import com.darlandi.carpoolingchallenge.repository.JourneyRepository;
import com.darlandi.carpoolingchallenge.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Class to assign cars with available seats to journeys.
 */
@Component
public class SeatDispatcherService {
    private static final Logger logger = LoggerFactory.getLogger(SeatDispatcherService.class);

    @Autowired
    private JourneyRepository journeyRepository;


    @Autowired
    private CarPoolingRepository carPoolingRepository;

    /**
     * Save the cars in the Redis DB depending on its available seats.
     *
     * @param car Object.
     */
    public void saveAvailableSeats(Car car) {
        carPoolingRepository.create(car.getAvailableSeats(), car);
    }

    /**
     * Update the car in DB with the new available seats.
     *
     * @param car            Object.
     * @param availableSeats Number of the new available seats.
     */
    public void updateAvailableSeats(Car car, int availableSeats) {
        carPoolingRepository.update(availableSeats, car);
    }

    /**
     * Search a car (with seats available) for a journey. Update the journey
     * with the ID of the car assigned, and update the new seats available in the car.
     * Try to match the same people and seats available, if it is not possible,
     * search in the next car with more seats available.
     *
     * @param journey Journey object.
     */
    public void assignAvailableCar(Journey journey) throws NoCarAvailableException {
        int numPeople = journey.getPeople();
        Optional<Car> carAvailable = Optional.empty();
        int seats = numPeople;

        for (; seats <= Constants.MAX_SEATS; seats++) {
            carAvailable = carPoolingRepository.getCarSeatsAvailable(seats);
            if (carAvailable.isPresent()) {
                updateAvailableSeats(carAvailable.get(), seats - numPeople);
                journey.setCarId(carAvailable.get().getId());
                journeyRepository.update(journey);
                journeyRepository.removeWaitingList(journey.getId());
                logger.info("Car ID " + carAvailable.get().getId() + " assigned to the journey ID " + journey.getId());
                break;
            }
        }
        if (carAvailable.isEmpty()) {
            throw new NoCarAvailableException();
        }
    }
}

