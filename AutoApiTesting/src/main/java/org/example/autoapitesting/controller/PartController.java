package org.example.autoapitesting.controller;

import org.example.autoapitesting.model.Part;
import org.example.autoapitesting.service.PartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/parts")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @GetMapping
    public ResponseEntity<List<Part>> getAll() {
        List<Part> parts = partService.getAllParts();
        return ResponseEntity.ok(parts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Part> getById(@PathVariable Long id) {
        return partService.getPartById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Part> create(@RequestBody Part part) {
        Part created = partService.createPart(part);
        return ResponseEntity.ok(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelPart(@PathVariable Long id) {
        Optional<Part> partOpt = partService.getPartById(id);
        if (partOpt.isPresent()) {
            Part part = partOpt.get();
            if ("CANCELLED".equals(part.getStatus())) {
                return ResponseEntity.status(409).build(); // Конфлікт, якщо вже скасовано
            }
            boolean success = partService.cancelPart(id);
            if (success) {
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.status(409).build();
        }
        return ResponseEntity.notFound().build();
    }
}