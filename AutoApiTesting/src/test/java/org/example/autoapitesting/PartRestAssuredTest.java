package org.example.autoapitesting;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.example.autoapitesting.model.Part;
import org.example.autoapitesting.repository.PartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PartRestAssuredTest {

    @LocalServerPort
    private int port;

    @Autowired
    private PartRepository partRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        partRepository.deleteAll();
    }

    @Test
    void testGetAllParts() {
        Part part = new Part(null, "Brake Disc", 3500.0, "Brakes", 8, "ACTIVE");
        partRepository.save(part);

        given()
                .when()
                .get("/api/parts")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].name", equalTo("Brake Disc"));
    }

    @Test
    void testGetPartById() {
        Part part = new Part(null, "Alternator", 8500.0, "Electrical", 5, "ACTIVE");
        Part saved = partRepository.save(part);

        given()
                .when()
                .get("/api/parts/{id}", saved.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("Alternator"))
                .body("price", equalTo(8500.0f));
    }

    @Test
    void testCreatePart() {
        String newPartJson = "{\"name\":\"Headlight\",\"category\":\"Lighting\",\"price\":2400.0,\"stock\":6,\"status\":\"ACTIVE\"}";

        given()
                .contentType(ContentType.JSON)
                .body(newPartJson)
                .when()
                .post("/api/parts")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", equalTo("Headlight"))
                .body("price", equalTo(2400.0f));
    }

    @Test
    void testConflictRepeatedAction() {
        Part part = new Part(null, "Suspension Spring", 1800.0, "Suspension", 4, "CANCELLED");
        Part saved = partRepository.save(part);

        given()
                .when()
                .delete("/api/parts/{id}", saved.getId())
                .then()
                .statusCode(409); // Conflict, ???????? ?????? ??? ?????????
    }

    @Test
    void testStateUnchangedAfterConflict() {
        Part part = new Part(null, "Clutch Kit", 7500.0, "Transmission", 3, "CANCELLED");
        Part saved = partRepository.save(part);

        given()
                .when()
                .delete("/api/parts/{id}", saved.getId())
                .then()
                .statusCode(409);

        // ???????????, ?? ?????? ????????? ????????? ? ????
        Part rechecked = partRepository.findById(saved.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("CANCELLED", rechecked.getStatus());
    }

    @Test
    void testBusinessRuleParameterized() {
        partRepository.save(new Part(null, "Small Bolt", 50.0, "Fasteners", 100, "ACTIVE"));
        partRepository.save(new Part(null, "Turbocharger", 12000.0, "Engine", 1, "ACTIVE"));

        given()
                .when()
                .get("/api/parts")
                .then()
                .statusCode(200)
                .body("price", hasItems(50.0f, 12000.0f));
    }
}