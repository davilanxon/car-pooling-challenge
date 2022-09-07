package com.darlandi.carpoolingchallenge.services;

import com.darlandi.carpoolingchallenge.entities.CarDataTransferObject;
import com.darlandi.carpoolingchallenge.entities.Car;
import com.darlandi.carpoolingchallenge.exceptions.BadInputException;
import com.darlandi.carpoolingchallenge.repository.CarPoolingRepository;
import com.darlandi.carpoolingchallenge.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Car pooling service to check and save cars and some utils.
 */
@Service
public class CarPoolingService {
    private static final String KEY_ID = "ID";

    @Autowired
    private SeatDispatcherService seatDispatcherService;

    @Autowired
    private CarPoolingRepository carPoolingRepository;

    /**
     * Check if the list of cars given is correct. If it is correct, then save
     * all the cars in the DB depending on the seats available.
     * If a car do not meet the min and max seats' requirement, or the ID is repeated
     * then delete all the DB and throw Exception.
     *
     * @param carList JSON list with all the cars.
     */
    public void register(List<CarDataTransferObject> carList) throws BadInputException {
        Set<Long> registeredId = new HashSet<>();
        for (CarDataTransferObject carDTO : carList) {
            if (carDTO.getId() == null || carDTO.getSeats() == null ||
                    carDTO.getSeats() < Constants.MIN_SEATS || carDTO.getSeats() > Constants.MAX_SEATS ||
                    registeredId.contains(carDTO.getId())) {
                registeredId.clear();
                carPoolingRepository.deleteAll();
                throw new BadInputException();
            }
            registeredId.add(carDTO.getId());
            Car newCar = new Car(carDTO.getId(), carDTO.getSeats());
            seatDispatcherService.saveAvailableSeats(newCar);
        }
        registeredId.clear();
    }

    /**
     * Convert the MultiValueMap input into a valid ID
     * @param mapId MultiValueMap
     * @return ID (long)
     * @throws BadInputException If the input is not correct.
     */
    public long mapToId(MultiValueMap<String, String> mapId) throws BadInputException {
        long id = 0;

        if (mapId.isEmpty() || !mapId.containsKey(KEY_ID) || mapId.get(KEY_ID).size() > 1) {
            throw new BadInputException();
        }
        try {
            id = Long.parseLong(mapId.getFirst(KEY_ID));
        } catch (NumberFormatException e) {
            throw new BadInputException();
        }
        return id;
    }
}
