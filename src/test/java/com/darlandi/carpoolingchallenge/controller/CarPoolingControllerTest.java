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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the basic commands of the Rest API.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(classes = TestRedisConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CarPoolingControllerTest {

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
    @DisplayName("Server OK")
    @Order(1)
    void testGetStatus() {
        ResponseEntity<String> response =
                testRestTemplate.getForEntity("/status", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Load a car list")
    @Order(2)
    void testLoadAvailableCars() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        String json = """
                [
                    {
                        "id": 1,
                        "seats": 4
                    },
                    {
                        "id": "2",
                        "seats": 5
                    }
                ]
                """;

        HttpEntity<String> request = new HttpEntity<>(json, headers);

        ResponseEntity<String> response = testRestTemplate.exchange("/cars", HttpMethod.PUT, request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Add some journeys")
    @Order(3)
    void testPeopleJourneys() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        String json = """
                  {
                    "id": 10%d,
                    "people": 4
                  }
                                
                """;
        //Add three journeys
        for (int i = 0; i < 3; i++) {
            String jsonMessage = String.format(json, i);
            HttpEntity<String> request = new HttpEntity<>(jsonMessage, headers);

            ResponseEntity<String> response =
                    testRestTemplate.exchange("/journey", HttpMethod.POST, request, String.class);
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Test
    @DisplayName("Locate the car given to a journey")
    @Order(4)
    void testLocate() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        String content = "ID=10%d";
        for (int i = 0; i < 3; i++) {
            String idMessage = String.format(content, i);
            HttpEntity<String> request = new HttpEntity<>(idMessage, headers);

            ResponseEntity<CarDataTransferObject> response =
                    testRestTemplate.exchange("/locate", HttpMethod.POST, request, CarDataTransferObject.class);
            CarDataTransferObject carResult = response.getBody();
            //First group should be in a car
            if (i == 0) {
                assertEquals(1L, carResult.getId());
                assertEquals(4, carResult.getSeats());
                assertEquals(HttpStatus.OK, response.getStatusCode());
            }
            //Also second group of people
            else if (i == 1){
                assertEquals(2L, carResult.getId());
                assertEquals(5, carResult.getSeats());
                assertEquals(HttpStatus.OK, response.getStatusCode());
            }
            //The third group should be waiting
            else if (i == 2){
                assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            }
        }
    }

    @Test
    @DisplayName("Droppoff a journey and try to give a car for the waiting journey")
    @Order(5)
    void testGiveCarWaitingJourney() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

        //Dropoff second journey
        String dropOffId = "ID=101";
        HttpEntity<String> request = new HttpEntity<>(dropOffId, headers);
        ResponseEntity<String> response =
                testRestTemplate.exchange("/dropoff", HttpMethod.POST, request, String.class);

        //Locate the third waiting group, now they should be in the second car
        String waitingJourney = "ID=102";
        request = new HttpEntity<>(waitingJourney, headers);
        ResponseEntity<CarDataTransferObject> response1 =
                testRestTemplate.exchange("/locate", HttpMethod.POST, request, CarDataTransferObject.class);
        CarDataTransferObject carResult = response1.getBody();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(2L, carResult.getId());
        assertEquals(5, carResult.getSeats());
    }
}