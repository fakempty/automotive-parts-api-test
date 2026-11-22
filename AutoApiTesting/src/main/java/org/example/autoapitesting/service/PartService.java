package org.example.autoapitesting.service;

import org.example.autoapitesting.model.Part;
import org.example.autoapitesting.repository.PartRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PartService {

    private final PartRepository partRepository;

    public PartService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    public List<Part> getAllParts() {
        return partRepository.findAll();
    }

    public Optional<Part> getPartById(Long id) {
        return partRepository.findById(id);
    }

    public Part createPart(Part part) {
        if (part.getStatus() == null) {
            part.setStatus("ACTIVE");
        }
        return partRepository.save(part);
    }

    public boolean cancelPart(Long id) {
        Optional<Part> partOpt = partRepository.findById(id);
        if (partOpt.isPresent()) {
            Part part = partOpt.get();
            if ("CANCELLED".equals(part.getStatus())) {
                return false; // Повторне скасування повертає false (для тестів на конфлікт)
            }
            part.setStatus("CANCELLED");
            partRepository.save(part);
            return true;
        }
        return false;
    }
}