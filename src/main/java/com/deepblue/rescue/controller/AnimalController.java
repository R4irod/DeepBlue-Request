package com.deepblue.rescue.controller;

import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.dto.response.TreatmentEligibilityResponse;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.service.AnimalService;
import com.deepblue.rescue.service.TreatmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/animals")
public class AnimalController {

    private final AnimalService animalService;
    private final TreatmentService treatmentService;

    public AnimalController(AnimalService animalService,
                            TreatmentService treatmentService) {
        this.animalService = animalService;
        this.treatmentService = treatmentService;
    }

    // GET /api/animals/AN-001
    @GetMapping("/{animalCode}")
    public ResponseEntity<AnimalResponse> findByCode(
            @PathVariable String animalCode) {
        return ResponseEntity.ok(animalService.findByCode(animalCode));
    }

    // GET /api/animals/in-rehabilitation
    @GetMapping("/in-rehabilitation")
    public ResponseEntity<List<AnimalResponse>> findAnimalsInRehabilitation() {
        return ResponseEntity.ok(animalService.findAnimalsInRehabilitation());
    }

    // GET /api/animals/AN-001/treatments
    @GetMapping("/{animalCode}/treatments")
    public ResponseEntity<List<TreatmentResponse>> findTreatments(
            @PathVariable String animalCode) {
        return ResponseEntity.ok(treatmentService.findByAnimalCode(animalCode));
    }

    // GET /api/animals/AN-001/treatment-eligibility
    @GetMapping("/{animalCode}/treatment-eligibility")
    public ResponseEntity<TreatmentEligibilityResponse> canReceiveTreatment(
            @PathVariable String animalCode) {

        boolean eligible = animalService.canReceiveTreatment(animalCode);

        return ResponseEntity.ok(
                new TreatmentEligibilityResponse(animalCode, eligible)
        );
    }
}