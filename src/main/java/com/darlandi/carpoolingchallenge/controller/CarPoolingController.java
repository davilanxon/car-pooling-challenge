package com.darlandi.carpoolingchallenge.controller;

import com.darlandi.carpoolingchallenge.entities.CarDataTransferObject;
import com.darlandi.carpoolingchallenge.entities.JourneyDataTransferObject;
import com.darlandi.carpoolingchallenge.exceptions.BadInputException;
import com.darlandi.carpoolingchallenge.exceptions.NoCarAvailableException;
import com.darlandi.carpoolingchallenge.exceptions.NoCarFoundException;
import com.darlandi.carpoolingchallenge.exceptions.NoJourneyFoundException;
import com.darlandi.carpoolingchallenge.repository.CarPoolingRepository;
import com.darlandi.carpoolingchallenge.repository.JourneyRepository;
import com.darlandi.carpoolingchallenge.services.CarPoolingService;
import com.darlandi.carpoolingchallenge.services.JourneyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Rest Controller
 */
@RestController
public class CarPoolingController {
    private static final Logger logger = LoggerFactory.getLogger(CarPoolingController.class);
    @Autowired
    private CarPoolingService carPoolingService;

    @Autowired
    private CarPoolingRepository carPoolingRepository;

    @Autowired
    private JourneyRepository journeyRepository;

    @Autowired
    private JourneyService journeyService;


    /**
     * Indicate the service has started up correctly and is ready to accept requests.
     *
     * @return 200 OK When the service is ready to receive requests.
     */
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok().build();
    }

    /**
     * Load the list of available cars in the service and remove all previous data
     * (reset the application state). This method may be called more than once during
     * the life cycle of the service.
     *
     * @param carList JSON
     * @return 200 OK When the list is registered correctly.
     * <p>
     * 400 Bad Request When there is a failure in the request format, expected
     * headers, or the payload can't be unmarshalled.
     */
    @PutMapping("/cars")
    public ResponseEntity<String> loadAvailableCars(@RequestBody List<CarDataTransferObject> carList) {
        carPoolingRepository.deleteAll();
        journeyRepository.deleteAll();
        logger.info("All repositories cleared.");
        try {
            carPoolingService.register(carList);
        } catch (BadInputException e) {
            logger.error(e + " Error registering the car list, the input is not correct or an ID is repeated.");
            return ResponseEntity.badRequest().build();
        }
        logger.info("All cars correctly saved in DB.");
        return ResponseEntity.ok().build();
    }

    /**
     * A group of people requests to perform a journey.
     *
     * @param journeyDTO JSON
     * @return 200 OK or 202 Accepted When the group is registered correctly
     * <p>
     * 400 Bad Request When there is a failure in the request format or the
     * payload can't be unmarshalled.
     */
    @PostMapping("/journey")
    public ResponseEntity<String> peopleJourney(@RequestBody JourneyDataTransferObject journeyDTO) {
        try {
            journeyService.register(journeyDTO);
        } catch (BadInputException e) {
            logger.error(e + " Error registering the journey ID " + journeyDTO.getId() + ", the input is not correct or it has already been saved.");
            return ResponseEntity.badRequest().build();
        } catch (NoCarAvailableException e) {
            journeyRepository.addToWaitingList(journeyDTO.getId());
            logger.error(e + " There is no car available for the journey ID " + journeyDTO.getId());
            logger.info(e + " Journey ID " + journeyDTO.getId() + " added to waiting list.");
        }
        logger.info("Journey ID " + journeyDTO.getId() +" correctly saved in DB.");
        return ResponseEntity.ok().build();
    }

    /**
     * A group of people requests to be dropped off. Whether they traveled or not.
     *
     * @param journeyID application/x-www-form-urlencoded
     * @return 200 OK or 204 No Content When the group is unregistered correctly.
     * <p>
     * 404 Not Found When the group is not to be found.
     * <p>
     * 400 Bad Request When there is a failure in the request format or the
     * payload can't be unmarshalled.
     */
    @PostMapping(value = "/dropoff", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> dropoff(@RequestBody MultiValueMap<String, String> journeyID) {
        Long id = null;

        try {
            id = carPoolingService.mapToId(journeyID);
            journeyService.dropOffGroup(id);
        } catch (BadInputException e) {
            logger.error(e + " Error unregistering the journey ID " + id + ", the input is not correct or it has been already deleted.");
            return ResponseEntity.badRequest().build();
        } catch (NoJourneyFoundException e) {
            logger.error(e + " Error unregistering the journey ID " + id + ", no journey has been found in DB.");
            return ResponseEntity.notFound().build();
        } catch (NoCarFoundException e) {
            logger.error(e + " Error unregistering the journey ID " + id + ", no car has been found in DB.");
            return ResponseEntity.notFound().build();
        }
        journeyService.checkWaitingJourneys();
        logger.info("Journey ID "+ id + " correctly unregistered and waiting journeys checked.");
        return ResponseEntity.ok().build();
    }


    /**
     * Given a group ID such that ID=X, return the car the group is traveling
     * with, or no car if they are still waiting to be served
     *
     * @param journeyID application/x-www-form-urlencoded
     * @return 200 OK With the car as the payload when the group is assigned to a car.
     * <p>
     * 204 No Content When the group is waiting to be assigned to a car.
     * <p>
     * 404 Not Found When the group is not to be found.
     * <p>
     * 400 Bad Request When there is a failure in the request format or the
     * payload can't be unmarshalled.
     */
    @PostMapping(value = "/locate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<CarDataTransferObject> locate(@RequestBody MultiValueMap<String, String> journeyID) {
        Long id = null;
        Optional<CarDataTransferObject> carDTOptional = null;

        try {
            id = carPoolingService.mapToId(journeyID);
            journeyService.journeyWaiting(id);
            carDTOptional = journeyService.getJourneyCar(id);
        } catch (BadInputException e) {
            logger.error(e + " Error locating the journey ID " + id + ", the input is not correct.");
            return ResponseEntity.badRequest().build();
        } catch (NoCarAvailableException e) {
            logger.warn(e + " The journey ID " + id + " is still waiting for a car.");
            return ResponseEntity.noContent().build();
        } catch (NoJourneyFoundException e) {
            logger.error(e + " Error, no journey ID " + id +" found in DB.");
            return ResponseEntity.notFound().build();
        }
        logger.info("Journey ID "+ id + " location successful.");
        return ResponseEntity.ok(carDTOptional.get());
    }
}
