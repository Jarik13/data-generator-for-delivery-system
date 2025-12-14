package org.example.model;

import lombok.Data;

@Data
public class DeliveryPoint {
    private String name;
    private String address;
    private String city;
    private String ref;
    private String typeRef;
}
