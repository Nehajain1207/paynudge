package com.neha.paynudge.model;

import jakarta.persistence.*;

@Entity
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String phone;

    protected Customer() {}
    public Customer(String name, String phone) { this.name = name; this.phone = phone; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
}
