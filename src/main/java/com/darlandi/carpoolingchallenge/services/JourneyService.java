package com.darlandi.carpoolingchallenge.services;

import com.darlandi.carpoolingchallenge.entities.Car;
import com.darlandi.carpoolingchallenge.entities.CarDataTransferObject;
import com.darlandi.carpoolingchallenge.entities.Journey;
import com.darlandi.carpoolingchallenge.entities.JourneyDataTransferObject;
import com.darlandi.carpoolingchallenge.exceptions.BadInputException;
import com.darlandi.carpoolingchallenge.exceptions.NoCarAvailableException;
import com.darlandi.carpoolingchallenge.exceptions.NoCarFoundException;
import com.darlandi.carpoolingchallenge.exceptions.NoJourneyFoundException;
import com.darlandi.carpoolingchallenge.repository.CarPoolingRepository;
import com.darlandi.carpoolingchallenge.repository.JourneyRepository;
import com.darlandi.carpoolingchallenge.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Class to check, save, unregister... and some utils for journeys.
 */
@Service
public class JourneyService {
    private static final Logger logger = LoggerFactory.getLogger(JourneyService.class);
    @Autowired
    private JourneyRepository journeyRepository;

    @Autowired
    private SeatDispatcherService seatDispatcherService;

    @Autowired
    private CarPoolingRepository carPoolingRepository;

    /**
     * Check if the journey introduced satisfy max and minimum people,
     * and if it is not already saved in the Redis DB.
     * If the journey is not found in the DB, then save it and try to find a car.
     *
     * @param journeyDTO JourneyDTO object.
     */

    public void register(JourneyDataTransferObject journeyDTO) throws BadInputException, NoCarAvailableException {
        Optional<Journey> journeyOptional = journeyRepository.get(journeyDTO.getId());
        if (journeyDTO.getId() == null || journeyDTO.getPeople() == null ||
                journeyDTO.getPeople() > Constants.MAX_PEOPLE ||
                journeyDTO.getPeople() < Constants.MIN_PEOPLE ||
                journeyOptional.isPresent()) {
            throw new BadInputException();
        }
        if (journeyOptional.isEmpty()) {
            Journey journey = new Journey(journeyDTO.getId(), journeyDTO.getPeople());
            journeyRepository.create(journey);
            if (journeyRepository.sizeWaitingList() == 0) {
                seatDispatcherService.assignAvailableCar(journey);
            } else {
                throw new NoCarAvailableException();
            }
        }
    }


    /**
     * Free the car associated with the journey and remove the journey from the DB.
     *
     * @param id ID of the journey.
     */

    public void dropOffGroup(Long id) throws NoJourneyFoundException, NoCarFoundException {
        Optional<Journey> journeyOptional = journeyRepository.get(id);
        if (journeyOptional.isPresent()) {
            if (journeyOptional.get().getCarId() != null) {
                Optional<Car> carOptional = carPoolingRepository.get(journeyOptional.get().getCarId());
                if (carOptional.isPresent()) {
                    seatDispatcherService.updateAvailableSeats(carOptional.get(),
                            carOptional.get().getAvailableSeats() + journeyOptional.get().getPeople());
                } else {
                    throw new NoCarFoundException();
                }
            } else {
                journeyRepository.removeWaitingList(id);
            }
            journeyRepository.delete(id);
        } else {
            throw new NoJourneyFoundException();
        }
    }


    /**
     * Check the first waiting journey and try to find a car for it. Keep the order
     * of arrival, but if there is no car available, add a waiting weight to that
     * journey and try to check the next waiting journey in the list.
     */
    public void checkWaitingJourneys() {
        Optional<Journey> journeyOptional = journeyRepository.getFirstWaiting();
        if (journeyOptional.isPresent()) {
            Journey journeyWaiting = journeyOptional.get();
            try {
                seatDispatcherService.assignAvailableCar(journeyWaiting);
            } catch (NoCarAvailableException e) {
                logger.warn(e + " There is no car with enough seats available for the waiting journey ID: " + journeyWaiting.getId());
                addWeightToJourney(journeyWaiting);
                checkWeightWaitingJourneys(journeyWaiting);
            }
        }
    }

    /**
     * Check the waiting weight of the first waiting journey, if after some tries (limit)
     * it is not possible to assign it a car, then check all the waiting list, but if
     * there is no car either for the next waiting journey, add a weight to penalize it.
     *
     * @param journeyWaiting First waiting journey.
     */
    private void checkWeightWaitingJourneys(Journey journeyWaiting) {
        double limit = carPoolingRepository.totalSize() * 0.3;
        if (journeyWaiting.getWaitingWeight() > limit) {
            List<Long> waitingJourneysIds = journeyRepository.getAllWaitingIds();
            for (int i = 1; i < waitingJourneysIds.size(); i++) {
                Optional<Journey> nextWaitingJourney = journeyRepository.get(waitingJourneysIds.get(i));
                if (nextWaitingJourney.isPresent()) {
                    try {
                        seatDispatcherService.assignAvailableCar(nextWaitingJourney.get());
                        break;
                    } catch (NoCarAvailableException e) {
                        logger.warn(e + " There is no car with enough seats available for the waiting journey ID: " + journeyWaiting.getId());
                        addWeightToJourney(nextWaitingJourney.get());
                    }
                }
            }
        }
    }

    /**
     * Add a waiting weight to a journey.
     *
     * @param journey Journey object.
     */
    private void addWeightToJourney(Journey journey) {
        int weight = journey.getWaitingWeight() + 1;
        journey.setWaitingWeight(weight);
        journeyRepository.update(journey);
    }

    /**
     * Get the car object associated with the journeyId.
     *
     * @param journeyId ID of the journey to search.
     * @return Optional CarDataTransferObject.
     */
    public Optional<CarDataTransferObject> getJourneyCar(Long journeyId) throws NoJourneyFoundException {
        Optional<Journey> journeyOptional = journeyRepository.get(journeyId);
        if (journeyOptional.isPresent()) {
            Optional<Car> carOptional = carPoolingRepository.get(journeyOptional.get().getCarId());
            CarDataTransferObject carDTO = new CarDataTransferObject(carOptional.get().getId(),
                    carOptional.get().getSeats());
            return Optional.of(carDTO);
        }
        throw new NoJourneyFoundException();
    }

    /**
     * Check if the given ID journey is waiting for a car
     *
     * @param journeyId ID of the journey
     * @throws NoCarAvailableException If there is no car assigned to the journey
     */
    public void journeyWaiting(Long journeyId) throws NoCarAvailableException {
        Optional<Journey> journeyOptional = journeyRepository.get(journeyId);
        if (journeyOptional.isPresent()) {
            if (journeyOptional.get().getCarId() == null) {
                throw new NoCarAvailableException();
            }
        }
    }
}
