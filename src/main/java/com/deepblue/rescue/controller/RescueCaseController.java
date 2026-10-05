package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.service.RescueCaseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rescue-cases")
public class RescueCaseController {

    private final RescueCaseService service;

    public RescueCaseController(RescueCaseService service) {
        this.service = service;
    }

    // GET /api/rescue-cases/RES-2026-001
    @GetMapping("/{caseCode}")
    public ResponseEntity<RescueCaseResponse> findByCode(
            @PathVariable String caseCode) {
        return ResponseEntity.ok(service.findByCode(caseCode));
    }

    // GET /api/rescue-cases?status=IN_REHABILITATION
    @GetMapping
    public ResponseEntity<List<RescueCaseResponse>> findByStatus(
            @RequestParam RescueStatus status) {
        return ResponseEntity.ok(service.findByStatus(status));
    }

    // PATCH /api/rescue-cases/RES-2026-001/status
    @PatchMapping("/{caseCode}/status")
    public ResponseEntity<RescueCaseResponse> changeStatus(
            @PathVariable String caseCode,
            @Valid @RequestBody ChangeRescueStatusRequest request) {
        return ResponseEntity.ok(service.changeStatus(caseCode, request));
    }
}