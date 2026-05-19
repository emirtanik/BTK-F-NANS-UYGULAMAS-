package com.finportfolio.controller;

import com.finportfolio.dto.SimulationRequest;
import com.finportfolio.dto.SimulationResponse;
import com.finportfolio.service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "5. Simülatör", description = "'Ne olurdu eğer?' yatırım simülasyonu")
public class SimulationController {

    private final SimulationService simulationService;

    /**
     * "Ne olurdu eger" simulatoru.
     *
     * Ornek istek:
     * POST /api/simulation
     * {
     *   "assetSymbol": "BTC",
     *   "investmentTry": 10000,
     *   "investmentDate": "2025-01-01"
     * }
     */
    @PostMapping
    public ResponseEntity<SimulationResponse> simulate(@Valid @RequestBody SimulationRequest request) {
        return ResponseEntity.ok(simulationService.simulate(request));
    }
}