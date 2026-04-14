package com.example.stadiumflow.controller;

import com.example.stadiumflow.domain.ConcessionOrder;
import com.example.stadiumflow.dto.GenericResponse;
import com.example.stadiumflow.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

/**
 * Controller for managing high-volume stadium concession orders.
 * Optimized for both online and offline (mesh-mode) transaction persistence.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Persists a new concession order to the stadium ledger.
     * 
     * @param order The validated order payload.
     * @return A ResponseEntity with a GenericResponse DTO.
     */
    @PostMapping
    public ResponseEntity<GenericResponse> placeOrder(@Valid @RequestBody ConcessionOrder order) {
        ConcessionOrder saved = orderRepository.save(order);
        log.info("Transaction Secured: Order #{} for {} stored successfully (MeshMode: {}).", 
            saved.getId(), saved.getItemMenu(), saved.isOfflineMesh());
        return ResponseEntity.ok(new GenericResponse("success", "Transaction verified and stored in stadium ledger."));
    }
}
