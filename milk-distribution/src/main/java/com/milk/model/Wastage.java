package com.milk.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Milk lost (spoiled, spilled, sample, etc.) - reduces stock without a sale. */
@Entity
public class Wastage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate wasteDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    private String reason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getWasteDate() { return wasteDate; }
    public void setWasteDate(LocalDate wasteDate) { this.wasteDate = wasteDate; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
