package com.neha.paynudge.eval;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/evals")
public class EvalController {
    private final EvalService evals;

    public EvalController(EvalService evals) { this.evals = evals; }

    @PostMapping("/run")
    public Map<String, Object> run() {
        boolean started = evals.start();
        return Map.of("started", started);
    }

    @GetMapping("/status")
    public Map<String, Object> status() { return evals.status(); }
}
