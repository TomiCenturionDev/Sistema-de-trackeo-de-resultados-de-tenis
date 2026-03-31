package com.tomas.tenis.stats.salesforce.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SalesforceLead {
    // Getters y Setters (Obligatorios para que Spring los convierta a JSON)
    private String FirstName;
    private String LastName;
    private String Company;
    private String Status;

    public SalesforceLead(String firstName, String lastName, String company, String status) {
        this.FirstName = firstName;
        this.LastName = lastName;
        this.Company = company;
        this.Status = status;
    }

}