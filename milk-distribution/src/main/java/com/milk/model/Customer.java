package com.milk.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    private String phone;

    private String location;

    /** Litres delivered every day by default. */
    @NotNull(message = "Enter the daily quantity")
    @DecimalMin(value = "0.0", message = "Cannot be negative")
    @Column(precision = 10, scale = 2)
    private BigDecimal dailyQuantity = BigDecimal.ZERO;

    @NotNull(message = "Enter the rate")
    @DecimalMin(value = "0.0", message = "Cannot be negative")
    @Column(precision = 10, scale = 2)
    private BigDecimal ratePerLitre = BigDecimal.ZERO;

    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public BigDecimal getDailyQuantity() { return dailyQuantity; }
    public void setDailyQuantity(BigDecimal dailyQuantity) { this.dailyQuantity = dailyQuantity; }
    public BigDecimal getRatePerLitre() { return ratePerLitre; }
    public void setRatePerLitre(BigDecimal ratePerLitre) { this.ratePerLitre = ratePerLitre; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
