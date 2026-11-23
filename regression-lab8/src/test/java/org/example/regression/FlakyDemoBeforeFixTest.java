package org.example.regression;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlakyDemoBeforeFixTest {

    private static final Map<String, String> SHARED_STORE = new HashMap<>();
    private static final AtomicInteger RUN_COUNTER = new AtomicInteger(0);

    @Test
    @Order(1)
    void createsSharedRecordWithoutCleanup() {
        SHARED_STORE.put("FIXED_KEY", "CREATED_BY_FIRST");
        assertEquals("CREATED_BY_FIRST", SHARED_STORE.get("FIXED_KEY"));
    }

    @Test
    @Order(2)
    void dependsOnOrderAndSharedState() {
        assertEquals("CREATED_BY_FIRST", SHARED_STORE.get("FIXED_KEY"),
                "Flaky failure: depends on order and shared state from previous test");
    }

    @Test
    @Order(3)
    void simulatesTimingAndNonDeterministicState() throws InterruptedException {
        Thread.sleep(50);
        int run = RUN_COUNTER.incrementAndGet();
        List<String> items = new ArrayList<>(List.of("PART-2", "PART-1"));
        if (run % 2 == 0) {
            items.add("EXTRA-DIRTY-PART");
        }
        assertEquals(List.of("PART-1", "PART-2"), items,
                "Flaky failure on even run due to dirty/unisolated state");
    }
}