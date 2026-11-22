package org.example.autoapitesting;

import org.example.autoapitesting.model.Part;
import org.example.autoapitesting.repository.PartRepository;
import org.example.autoapitesting.service.PartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PartServiceTest {

    @Autowired
    private PartService partService;

    @Autowired
    private PartRepository partRepository;

    @BeforeEach
    void setUp() {
        partRepository.deleteAll();
    }

    @Test
    void testCreatePartSuccess() {
        Part part = new Part(null, "Brake Pad", 2400.0, "Brakes", 15, "ACTIVE");
        Part created = partService.createPart(part);

        assertNotNull(created.getId());
        assertEquals("Brake Pad", created.getName());
        assertEquals("ACTIVE", created.getStatus());
    }

    @ParameterizedTest
    @CsvSource({
            "Oil Filter, 450.0, Engine",
            "Spark Plug, 320.0, Ignition",
            "Air Filter, 280.0, Filtration"
    })
    void testCreatePartParameterized(String name, Double price, String category) {
        Part part = new Part(null, name, price, category, 10, "ACTIVE");
        Part created = partService.createPart(part);

        assertNotNull(created.getId());
        assertEquals(name, created.getName());
        assertEquals(price, created.getPrice());
        assertEquals("ACTIVE", created.getStatus());
    }

    @Test
    void testGetPartById() {
        Part part = new Part(null, "Alternator", 8500.0, "Electrical", 5, "ACTIVE");
        Part saved = partRepository.save(part);

        Optional<Part> found = partService.getPartById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Alternator", found.get().getName());
    }

    @Test
    void testCancelAlreadyCancelledPart() {
        Part part = new Part(null, "Radiator", 4200.0, "Cooling", 3, "CANCELLED");
        Part saved = partRepository.save(part);

        boolean result = partService.cancelPart(saved.getId());
        assertFalse(result);
    }
}