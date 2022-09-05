package com.darlandi.carpoolingchallenge.controller;

import com.darlandi.carpoolingchallenge.controller.embeddedRedisConfigurationTest.TestRedisConfiguration;
import com.darlandi.carpoolingchallenge.entities.CarDataTransferObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * These tests allow to check the following case: There are 4 cars with only 4 seats,
 * first they are fill with groups of 1 person, and later come two groups of 5 people
 * (which obviously can't be served and block the waiting list) and a last group
 * of 1 people. Then, only a group of 1 person leave a car, and although the last group
 * could be served, it won't, because there is a "blocking state" that waits some
 * "dropoff" depending on the size of the car list. So then, when another "dropoff"
 * happens, the "blocking state" is broken and all the waiting list is checked to find
 * a car for a journey.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(classes = TestRedisConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CarPoolingPriorityTest {

    private TestRestTemplate testRestTemplate;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        restTemplateBuilder = restTemplateBuilder.rootUri("http://localhost:" + port);
        testRestTemplate = new TestRestTemplate(restTemplateBuilder);
    }

    @Test
    @DisplayName("Load the car list")
    @Order(2)
    void testLoadAvailableCars() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        String json = """
                [
                 {"id": 1,"seats": 4},
                 {"id": 2,"seats": 4},
                 {"id": 3,"seats": 4},
                 {"id": 4,"seats": 4}
                ]
                """;

        HttpEntity<String> request = new HttpEntity<>(json, headers);

        ResponseEntity<String> response = testRestTemplate.exchange("/cars", HttpMethod.PUT, request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Add some journeys")
    @Order(3)
    void testAddSomeJourneys() {
        ResponseEntity<String> response = null;
        ResponseEntity<String> response1 = null;
        ResponseEntity<String> response2;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        String json = """
                  {"id": %d,"people": 1}
                """;
        String json1 = """
                  {"id": %d,"people": 5}
                """;
        String json2 = """
                  {"id": %d,"people": 1}         
                """;
        //Fill the 4 cars of 4 seats with groups of 1
        int i = 0;
        for (; i < (4 * 4); i++) {
            String jsonMessage = String.format(json, i);
            HttpEntity<String> request = new HttpEntity<>(jsonMessage, headers);
            response = testRestTemplate.exchange("/journey", HttpMethod.POST, request, String.class);
        }
        //Add 2 Waiting groups of 5 people
        for (; i < ((4 * 4) + 2); i++) {
            String jsonMessage = String.format(json1, i);
            HttpEntity<String> request = new HttpEntity<>(jsonMessage, headers);
            response1 = testRestTemplate.exchange("/journey", HttpMethod.POST, request, String.class);
        }
        //Last waiting group of 1 people
        String jsonMessage = String.format(json2, i);
        HttpEntity<String> request = new HttpEntity<>(jsonMessage, headers);
        response2 = testRestTemplate.exchange("/journey", HttpMethod.POST, request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
    }

    @Test
    @DisplayName("Locate all the journeys")
    @Order(4)
    void testLocateAllJourneys() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        String content = "ID=%d";
        for (int i = 0; i < 19; i++) {
            String idMessage = String.format(content, i);
            HttpEntity<String> request = new HttpEntity<>(idMessage, headers);

            ResponseEntity<CarDataTransferObject> response =
                    testRestTemplate.exchange("/locate", HttpMethod.POST, request, CarDataTransferObject.class);
            CarDataTransferObject carResult = response.getBody();
            if (i < 16) {
                assertEquals(HttpStatus.OK, response.getStatusCode());
            } else if (i >= 16) {
                assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            }
        }
    }

    @Test
    @DisplayName("Dropoff 2 groups of 1 person and try to give a car")
    @Order(5)
    void testDropOffJourneysGiveCar() {
        ResponseEntity<String> response = null;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        //First dropoff 1 group of 1 person
        String content = "ID=0";
        HttpEntity<String> request = new HttpEntity<>(content, headers);
        response = testRestTemplate.exchange("/dropoff", HttpMethod.POST, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        //Locate the last group of 2 people, as the "blocking state" has not been broken yet,
        //there shouldn't be any car available.
        content = "ID=18";
        request = new HttpEntity<>(content, headers);
        ResponseEntity<CarDataTransferObject> response1 =
                testRestTemplate.exchange("/locate", HttpMethod.POST, request, CarDataTransferObject.class);
        assertEquals(HttpStatus.NO_CONTENT, response1.getStatusCode());

        //Then dropoff again another 1 group of 1 person
        content = "ID=1";
        request = new HttpEntity<>(content, headers);
        response = testRestTemplate.exchange("/dropoff", HttpMethod.POST, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        //Finally, test if last group of 2 people found a car because the "blocking state"
        //has been broken.
        content = "ID=18";
        request = new HttpEntity<>(content, headers);
        response1 = testRestTemplate.exchange("/locate", HttpMethod.POST, request, CarDataTransferObject.class);
        assertEquals(HttpStatus.OK, response1.getStatusCode());
    }
}