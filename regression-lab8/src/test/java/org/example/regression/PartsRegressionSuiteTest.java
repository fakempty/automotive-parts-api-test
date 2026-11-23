package org.example.regression;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PartsRegressionSuiteTest {

    private PartsApiClient client;
    private final List<String> createdPartIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        String baseUrl = System.getProperty("sut.baseUrl", "http://localhost:8080");
        client = new PartsApiClient(baseUrl);
    }

    @AfterEach
    void tearDown() {
        for (String id : createdPartIds) {
            client.deletePartIfExists(id);
        }
        createdPartIds.clear();
    }

    @SmokeTest
    void smokeApiHealthAndListParts() throws Exception {
        HttpResponse<String> response = client.getAllParts();
        assertEquals(200, response.statusCode(), "GET /api/parts must return HTTP 200");
        assertNotNull(response.body());
    }

    @SmokeTest
    void smokeCreateAndGetPartById() throws Exception {
        String uniquePartNumber = "PN-SMOKE-" + UUID.randomUUID();
        HttpResponse<String> createResp = client.createPart(uniquePartNumber, "Oil Filter", 25.50);
        assertTrue(createResp.statusCode() == 200 || createResp.statusCode() == 201,
                "Expected 200 or 201 on create, got: " + createResp.statusCode());

        String id = PartsApiClient.extractId(createResp.body());
        assertNotNull(id, "Created part ID must be returned in JSON response");
        createdPartIds.add(id);

        HttpResponse<String> getResp = PartsApiClient.awaitCondition(
                () -> {
                    try {
                        return client.getPartById(id);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                resp -> resp.statusCode() == 200 && resp.body().contains(uniquePartNumber),
                Duration.ofSeconds(5),
                Duration.ofMillis(100)
        );

        assertEquals(200, getResp.statusCode());
        assertTrue(getResp.body().contains(uniquePartNumber));
    }

    @RegressionTest
    void regressionIsolatedDataNoCollisionOnRepeatedRuns() throws Exception {
        String uniquePartNumber = "PN-REG-" + UUID.randomUUID();
        HttpResponse<String> createResp = client.createPart(uniquePartNumber, "Brake Pad", 60.00);
        assertTrue(createResp.statusCode() == 200 || createResp.statusCode() == 201);

        String id = PartsApiClient.extractId(createResp.body());
        if (id != null) {
            createdPartIds.add(id);
        }

        HttpResponse<String> listResp = client.getAllParts();
        assertEquals(200, listResp.statusCode());
        assertTrue(listResp.body().contains(uniquePartNumber));
    }

    @RegressionTest
    void regressionDeterministicOrderingAndFiltering() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String pnB = "PN-B-" + suffix;
        String pnA = "PN-A-" + suffix;

        HttpResponse<String> respB = client.createPart(pnB, "Spark Plug B", 15.00);
        HttpResponse<String> respA = client.createPart(pnA, "Spark Plug A", 12.00);

        String idB = PartsApiClient.extractId(respB.body());
        String idA = PartsApiClient.extractId(respA.body());
        if (idB != null) createdPartIds.add(idB);
        if (idA != null) createdPartIds.add(idA);

        List<String> sortedPartNumbers = List.of(pnB, pnA).stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        assertEquals(List.of(pnA, pnB), sortedPartNumbers);
    }
}